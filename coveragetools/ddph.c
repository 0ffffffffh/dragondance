/*
    ddph (Coverage collector module for Intel PIN)
    Copyright (C) 2019  Oguz Kartal

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string>
using namespace std;
#include "pin.H"

typedef char CHAR;
typedef unsigned char BYTE;
typedef unsigned long U32;

#define MARK 'MARK'

typedef void(*LIST_RECORD_DESTROY_HANDLER)(VOID *record);

typedef struct __LIST_ENTRY
{
    struct __LIST_ENTRY *Next;
}LIST_ENTRY,*PLIST_ENTRY;

typedef struct __LIST_HEADER
{
    PLIST_ENTRY     Head;
    PLIST_ENTRY     Tail;
    USIZE           Count;
    PIN_LOCK        Lock;
}LIST_HEADER,*PLIST_HEADER;

#define OFFSET_OF(type,field) (USIZE)( &((type *)NULL)->field )
#define LIST_RECORD(entry, type, field) (type *)( ( (ADDRINT)(entry) ) + OFFSET_OF(type,field) )

typedef struct __TRACE_RANGE
{
    LIST_ENTRY  Entry;
    USIZE       RangeStart;
    USIZE       RangeEnd;
    USIZE       RangeSize;
    UINT16      ModuleId;
    UINT32      InstCount;
}TRACE_RANGE, *PTRACE_RANGE;

typedef struct __MEMORY 
{
    U32     Mark;
    USIZE   Size;
    BYTE    Mem[];
}MEMORY,*PMEMORY;

typedef struct __MODULE
{
    LIST_ENTRY  Entry;
    UINT16      Id;
    ADDRINT     Base;
    ADDRINT     End;
    CHAR        Path[PATH_MAX];
}MODULE,*PMODULE;

typedef struct __TLS_CONTEXT
{
    LIST_ENTRY      Entry;
    THREADID        Tid;
    BOOL            Active;
    PTRACE_RANGE    TraceRange;
}TLS_CONTEXT,*PTLS_CONTEXT;


#define TLSCTX() DppGetCurrentThreadTlsContext()


FILE *          DpTraceFd = NULL;
FILE *          DpCoverageDataFd = NULL;

LIST_HEADER     DpTraceList;
LIST_HEADER     DpModuleList;
LIST_HEADER     DpTlsList;

PTRACE_RANGE    DpGlobalTraceRange = NULL;

TLS_KEY         DpTlsKey = -1;

BOOL            DppLoggingEnabled = FALSE;
BOOL            DppOnlyTarget = TRUE;

BOOL            DppReducedCapture = TRUE;
UINT16          DppTargetImageId = 0;

#define iabs(v) v & 0x80000000 ? ~v + 1 : v

#define DLOG(x,...) if (DppLoggingEnabled) { \
                        fprintf(DpTraceFd,x "\n",##__VA_ARGS__); \
                    }

#define DASSERT(exp) if (!exp) DLOG("Expression failed. " #exp)

#define WCOVS(x,...) fprintf(DpCoverageDataFd,x, ##__VA_ARGS__)
#define WCOVSL(x,...) WCOVS(x "\n", ##__VA_ARGS__)
#define WCOVB(buf,size) fwrite(buf,size,1,DpCoverageDataFd)



#define EXPOSEMEM(addr) ((PMEMORY)(((USIZE)(addr)) -  sizeof(MEMORY)))
#define MEM2PTR(mem) (((USIZE)mem) + sizeof(MEMORY))

static KNOB<string> KnobOutputFile(KNOB_MODE_WRITEONCE, "pintool", "o", "ddph.out", "coverage data output file");
static KNOB<string> KnobWriteDdphLog(KNOB_MODE_WRITEONCE,"pintool","l","ddph.log","ddph logging option");
static KNOB<string> KnobPrecision(KNOB_MODE_WRITEONCE, "pintool", "p", "reduced", "coverage collection precision.");


VOID *DppMalloc(USIZE size)
{
    PMEMORY pmem;

    VOID *mem = malloc(size + sizeof(MEMORY));

    if (!mem)
        return NULL;


    memset(mem, 0, size + sizeof(MEMORY));

    pmem = (PMEMORY)mem;

    pmem->Mark = MARK;
    pmem->Size = size;

    return (VOID *)MEM2PTR(pmem);
}


void _DppFree(VOID **mem, BOOL zeroBeforeFree)
{
    PMEMORY pmem;

    if (!mem)
        return;

    if (!*mem)
        return;

    pmem = EXPOSEMEM(*mem);

    assert(pmem != NULL);
        
    if (pmem->Mark != MARK)
        return;

        
    if (zeroBeforeFree)
        memset(pmem, 0, pmem->Size + sizeof(MEMORY));

    free(pmem);

    //set null pointer to secure the memory's reference
    *mem = NULL;
}


#define DppFree(mem) _DppFree((VOID **)&mem,TRUE)


BOOL DppResizeMem(VOID **mem, INT32 amountBytes)
{
    PMEMORY pmem = NULL;
    VOID *newMem;

    if (!mem)
        return FALSE;

    if (!*mem)
        return FALSE;

    pmem = EXPOSEMEM(*mem);

    assert(pmem != NULL);

    if (pmem->Mark != MARK)
        return FALSE;

    if (iabs(amountBytes) > pmem->Size)
        return FALSE;
    //                      mem header   + old user size + expansion size
    newMem = realloc(pmem, sizeof(MEMORY) + pmem->Size + amountBytes);

    if (!newMem)
        return FALSE;

    pmem = (PMEMORY)newMem;

    //we don't count the header's overhead
    pmem->Size += amountBytes;

    *mem = (VOID *)MEM2PTR(pmem);

    return TRUE;
}


void DpInsertListTail(PLIST_HEADER list, PLIST_ENTRY entry)
{
    PLIST_ENTRY oldTail;

    PIN_GetLock(&list->Lock, 1);

    if (!list->Head)
    {
        list->Head = list->Tail = entry;
        list->Count = 1;
    }
    else
    {
        list->Tail->Next = entry;
        list->Tail = entry;
        entry->Next = NULL;
        list->Count++;
    }

    PIN_ReleaseLock(&list->Lock);
}

PLIST_ENTRY DppPopList(PLIST_HEADER list, BOOL gainLock)
{
    PLIST_ENTRY entry=NULL;

    if (gainLock)
        PIN_GetLock(&list->Lock, 1);

    entry = list->Head;

    if (!entry)
        goto quit;

    list->Head = entry->Next;
    entry->Next = NULL;

    if (!list->Head)
        list->Tail = NULL;

    list->Count--;

quit:

    if (gainLock)
        PIN_ReleaseLock(&list->Lock);

    return entry;
}


void DppInitList(PLIST_HEADER list)
{
    list->Head = list->Tail = NULL;
    list->Count = 0;
    PIN_InitLock(&list->Lock);
}

void DppDestroyList(PLIST_HEADER list, LIST_RECORD_DESTROY_HANDLER handler, USIZE entryOffset)
{
    PLIST_ENTRY entry;
    VOID *record;


    PIN_GetLock(&list->Lock, 1);

    while (entry = DppPopList(list,FALSE))
    {
        record = (VOID *)(((USIZE)entry) - entryOffset);
        handler(record);
    }

    PIN_ReleaseLock(&list->Lock);
}

void DpInitLists()
{
    DppInitList(&DpTraceList);
    DppInitList(&DpModuleList);
    DppInitList(&DpTlsList);
}

void DppGenericListRecordFree(VOID *r)
{
    DppFree(r);
}

void DppDestroyLists()
{
    DppDestroyList(&DpTraceList, DppGenericListRecordFree, OFFSET_OF(TRACE_RANGE, Entry));
    DppDestroyList(&DpModuleList, DppGenericListRecordFree, OFFSET_OF(MODULE, Entry));
    DppDestroyList(&DpTlsList, DppGenericListRecordFree, OFFSET_OF(TLS_CONTEXT, Entry));
}

PTRACE_RANGE DppNewTraceRange()
{
    PTRACE_RANGE pRange = (PTRACE_RANGE)DppMalloc(sizeof(TRACE_RANGE));

    if (!pRange)
        return NULL;

    DpInsertListTail(&DpTraceList, &pRange->Entry);

    return pRange;
}


PMODULE DpAddModule(ADDRINT base, ADDRINT end, UINT32 id, const CHAR *path)
{
    PMODULE pmod = (PMODULE)DppMalloc(sizeof(MODULE));


    if (!pmod)
        return NULL;

    DpInsertListTail(&DpModuleList, &pmod->Entry);

    
    pmod->Id = id;
    pmod->Base = base;
    pmod->End = end;
    strcpy(pmod->Path, path);

    return pmod;
}


#define DpInitTRange(tr,ia,is) (tr)->RangeStart = ia; \
                                (tr)->RangeEnd = ia + is; \
                                (tr)->RangeSize = is;

void DpHelp()
{
    printf(KnobOutputFile.StringKnobSummary().c_str());
}


PTLS_CONTEXT DppGetCurrentThreadTlsContext()
{
    PTLS_CONTEXT pTlsCtx = NULL;
    THREADID ctid;

    ctid = PIN_ThreadId();


    if (ctid == INVALID_THREADID)
    {
        DLOG("invalid tid");
        return NULL;
    }

    pTlsCtx = (PTLS_CONTEXT)PIN_GetThreadData(DpTlsKey, ctid);

    assert(pTlsCtx != NULL);

    return pTlsCtx;
}

PTLS_CONTEXT DppGetTlsContextFromTid(THREADID tid)
{
    PLIST_ENTRY entry;
    PTLS_CONTEXT pTls;

    PIN_GetLock(&DpTlsList.Lock, 1);

    entry = DpTlsList.Head;

    while (entry)
    {
        pTls = LIST_RECORD(entry, TLS_CONTEXT, Entry);
        
        if (pTls->Tid == tid)
            break;

        entry = entry->Next;
    }

    if (!entry)
        pTls = NULL;

    PIN_ReleaseLock(&DpTlsList.Lock);

    return pTls;
}


void DpImageLoaded(IMG img, VOID *v)
{
    UINT32 nr = 0;
    PMODULE pmod;
    
    pmod = DpAddModule(IMG_LowAddress(img), IMG_HighAddress(img),IMG_Id(img), IMG_Name(img).c_str());
    
    if (!pmod)
    {
        DLOG("no more room for module on the memory!");
        PIN_ExitApplication(EXIT_FAILURE);
        return;
    }

    DLOG("%s loaded (id: %d)", pmod->Path,pmod->Id);
    DLOG("start: %p, end: %p", pmod->Base, pmod->End);
    DLOG("--------");

    if (IMG_IsMainExecutable(img))
    {
        DppTargetImageId = (UINT16)IMG_Id(img);
        DLOG("target image id: %d", DppTargetImageId);
    }
}

BOOL DppGetModuleIdFromAddr(ADDRINT addr, UINT16 *pId)
{
    IMG img;
    
    PIN_LockClient();
    
    img = IMG_FindByAddress(addr);
    
    PIN_UnlockClient();

    if (!img.is_valid())
    {
        return FALSE;
    }
    
    *pId = (UINT16)IMG_Id(img);

    return TRUE;
}

BOOL DppGetModuleById(UINT16 id, PMODULE *pModule)
{
    BOOL found = FALSE;
    PMODULE mod;

    PIN_GetLock(&DpModuleList.Lock, 1);

    for (PLIST_ENTRY entry = DpModuleList.Head; entry != NULL; entry = entry->Next)
    {
        mod = LIST_RECORD(entry, MODULE, Entry);
        
        if (mod->Id == id)
        {
            *pModule = mod;
            found = TRUE;
            break;
        }
    }

    PIN_ReleaseLock(&DpModuleList.Lock);

    return found;

}

void DpThreadStartHandler(THREADID threadIndex, CONTEXT *ctxt, INT32 flags, VOID *v)
{
    PTLS_CONTEXT pTlsCtx = (PTLS_CONTEXT)DppMalloc(sizeof(TLS_CONTEXT));
    
    if (!pTlsCtx)
    {
        DLOG("Tls context mem alloc failed for tid %lu",threadIndex);
        return;
    }

    pTlsCtx->Tid = threadIndex;
    pTlsCtx->Active = TRUE;

    DpInsertListTail(&DpTlsList, &pTlsCtx->Entry);

    if (!PIN_SetThreadData(DpTlsKey, pTlsCtx, threadIndex))
    {
        DLOG("tls key invalid or not set thread data");
        DppFree(pTlsCtx);
        return;
    }

    pTlsCtx->TraceRange = DppNewTraceRange();

    DLOG("Thread(%d) started with TLS CONTEXT: %p (initial Rangeptr: %p)", threadIndex, pTlsCtx,pTlsCtx->TraceRange);
}

void DpThreadFinishHandler(THREADID threadIndex, const CONTEXT *ctxt, INT32 code, VOID *v)
{
    PTLS_CONTEXT pTlsCtx = DppGetTlsContextFromTid(threadIndex);

    DLOG("thread tid(%d) finished. tls: %p", threadIndex, pTlsCtx);

    if (!pTlsCtx)
        return;

    pTlsCtx->Active = FALSE;
}

void DppInstructionExecutionHandler(THREADID tid, ADDRINT insAddr, USIZE bsize, BOOL isBlock)
{
    UINT16 mid;
    PTLS_CONTEXT pTlsCtx = NULL,ptm=NULL;
    PTRACE_RANGE pRange = NULL;

    BOOL isGlobalRange = FALSE;

    pTlsCtx = TLSCTX();

    if (!pTlsCtx)
    {
        DLOG("NON TLS");
        DLOG("WARNING! no tls ctx for tid %lu", tid /*PIN_ThreadId()*/);
        DLOG("Going with globalrange");

        pRange = DpGlobalTraceRange;
        isGlobalRange = TRUE;
    }
    else
    {
        DASSERT(pTlsCtx->Tid == tid);
        pRange = pTlsCtx->TraceRange;
    }

    
    if (DppGetModuleIdFromAddr(insAddr, &mid))
    {
        if (DppOnlyTarget && DppTargetImageId != mid)
            return;
    }
    else
    {
        if (DppOnlyTarget)
            return;

        mid = 0xDEAD;
        DLOG("instruction came from unknown image (%p)", insAddr);
    }

    if (!pRange->RangeStart)
    {
        DpInitTRange(pRange, insAddr, bsize);

        pRange->ModuleId = mid;

        if (!isBlock)
            pRange->InstCount = 1;
    }
    else
    {
        //will the current instruction appendable to the latest range?
        if (insAddr == pRange->RangeEnd)
        {
            //yep append it
            pRange->RangeSize += bsize;
            pRange->RangeEnd += bsize;

            if (!isBlock)
                pRange->InstCount++;
        }
        else
        {
            pRange = DppNewTraceRange();

            if (!pRange)
            {
                DLOG("There is no more room for new tracerange");
                PIN_ExitApplication(OS_RETURN_CODE_NO_MEMORY);
                return;
            }

            if (isGlobalRange)
                DpGlobalTraceRange = pRange;
            else
                pTlsCtx->TraceRange = pRange;

            DpInitTRange(pRange, insAddr, bsize);

            pRange->ModuleId = mid;

            if (!isBlock)
                pRange->InstCount = 1;

        }
    }
}

void DpInstructionHandler(INS ins, VOID *v)
{
    INS_InsertCall(ins, IPOINT_BEFORE, (AFUNPTR)DppInstructionExecutionHandler,
        IARG_THREAD_ID,
        IARG_INST_PTR,
        IARG_UINT32, INS_Size(ins),
        IARG_BOOL, FALSE,
        IARG_END);
}

void DpTraceHandler(TRACE trace, VOID *v)
{
    BBL bbl;

    for (bbl = TRACE_BblHead(trace); BBL_Valid(bbl); bbl = BBL_Next(bbl))
    {
        BBL_InsertCall(bbl, IPOINT_BEFORE, (AFUNPTR)DppInstructionExecutionHandler,
            IARG_THREAD_ID,
            IARG_INST_PTR,
            IARG_UINT32, BBL_Size(bbl),
            IARG_BOOL, TRUE,
            IARG_END);
    }
}

void DpWriteCoverageData()
{
    PLIST_ENTRY entry;
    UINT32 rangeStartOffset;
    UINT16 rangeSize;
    ADDRINT imageBase;

    PMODULE module;
    PTRACE_RANGE range;

    //write module info first

    entry = DpModuleList.Head;
    
    WCOVSL("DDPH-PINTOOL");

    WCOVSL("EntryCount: %d, ModuleCount: %d", DpTraceList.Count, DpModuleList.Count);
    
    WCOVSL("Module table row names (left to right): Module Id,  Module Base, Module End, Module Path");
    WCOVSL("");

    WCOVSL("MODULE_TABLE");

    while (entry)
    {
        module = LIST_RECORD(entry, MODULE, Entry);

        WCOVSL("%d, %p, %p, %s", module->Id, module->Base, module->End, module->Path);

        entry = entry->Next;
    }

    WCOVSL("\nENTRY_TABLE");

    entry = DpTraceList.Head;

    while (entry)
    {
        range = LIST_RECORD(entry, TRACE_RANGE, Entry);
        
        if (!DppGetModuleById(range->ModuleId, &module))
        {
            if (range->RangeStart != NULL)
            {
                DLOG("module not found skipped, modid=%d, range=%p", range->ModuleId, range);
            }

            entry = entry->Next;
            continue;
        }

        rangeSize = (UINT16)(range->RangeEnd - range->RangeStart);
        rangeStartOffset = range->RangeStart - module->Base;

        WCOVB(&rangeStartOffset, sizeof(UINT32));
        WCOVB(&rangeSize, sizeof(UINT16));
        WCOVB(&range->ModuleId, sizeof(UINT16));
        WCOVB(&range->InstCount, sizeof(UINT32));

        entry = entry->Next;
    }

}

VOID DpFinish(INT32 code, VOID *v)
{
    DpWriteCoverageData();

    if (DpTraceFd)
    {
        fclose(DpTraceFd);
        DpTraceFd = NULL;
    }

    if (DpCoverageDataFd)
    {
        fclose(DpCoverageDataFd);
        DpCoverageDataFd = NULL;
    }

    DppDestroyLists();

    if (DpTlsKey != -1)
    {
        PIN_DeleteThreadDataKey(DpTlsKey);
        DpTlsKey = -1;
    }
}


#define MAX_BANNER_LINE 10
#define BANNER_LR_MARGIN 3

#define PUTCR(s,c) { for (int z=0;z<c;z++) CONSOLE_NOPREFIX(s); }
#define PUTC(s) CONSOLE_NOPREFIX(s)

#define FILLCHR " "
#define FRAMECHR "+"

void DppPrintBanner(UINT16 count, ...)
{
    USIZE maxLen=0, width=0, height=0,spos=0;

    struct {
        char *s;
        int len;
    }items[MAX_BANNER_LINE];

    va_list vl;
    char *item;

    if (count > MAX_BANNER_LINE)
        return;

    va_start(vl, count);

    for (int i = 0; i < count; i++)
    {
        item = (char *)va_arg(vl, char *);
        items[i].s = item;
        items[i].len = strlen(item);

        if (items[i].len > maxLen)
            maxLen = items[i].len;

    }

    va_end(vl);

    width = maxLen + (2 * (BANNER_LR_MARGIN + 1));
    height = count + 2;

    PUTCR(FRAMECHR, width);
    PUTC("\n");

    for (int i = 0; i < count; i++)
    {
        if (items[i].len == 0)
        {
            PUTC(FRAMECHR);
            PUTCR(FILLCHR, width - 2);
            PUTC(FRAMECHR);
            PUTC("\n");
        }
        else
        {
            spos = ((width / 2) - (items[i].len / 2)) - 1;

            PUTC(FRAMECHR);
            PUTCR(FILLCHR, spos);
            PUTC(items[i].s);
            PUTCR(FILLCHR, width - (spos + items[i].len) - 2);
            PUTC(FRAMECHR);
            PUTC("\n");
        }

    }

    PUTCR(FRAMECHR, width);
    PUTC("\n");

}

#define BUILD_DATE "Build datetime: " __DATE__ " " __TIME__

void DppShowBanner()
{
    DppPrintBanner(8,
        "",
        "Dragon Dance Pin Helper (ddph)",
        "Binary Coverage Data Collector",
        BUILD_DATE,
        "",
        "oguz kartal - 2019",
        "http://oguzkartal.net",
        "");

}

BOOL DppInit()
{   
    if (!_stricmp(KnobPrecision.Value().c_str(), "reduced"))
        DppReducedCapture = TRUE;
    else
        DppReducedCapture = FALSE;

    if (_stricmp(KnobWriteDdphLog.Value().c_str(),"no"))
    {
        DpTraceFd = fopen(KnobWriteDdphLog.Value().c_str(), "w+");

        if (!DpTraceFd)
        {
            PUTC("Log file could not be opened. You may need to run the pin with elevated permission.");
            return FALSE;
        }

        /*we don't want buffering because 
        we have to see the log output immediately when
        anything goes wrong. */

        setvbuf(DpTraceFd, NULL, _IONBF, 0);

        DppLoggingEnabled = TRUE;
    }

    DLOG("creating coverage data file.");

    DpCoverageDataFd = fopen(KnobOutputFile.Value().c_str(), "wb");

    if (!DpCoverageDataFd)
    {
        if (!DppLoggingEnabled)
            PUTC("Coverage data file could not be created. you may need to run the pin with elevated permission.");
        else
            DLOG("Coverage data file could not be created.");

        return FALSE;
    }

    DLOG("Init lists");
    DpInitLists();

    return TRUE;
}

int main(int argc, char **argv)
{
    DppShowBanner();

    PIN_InitSymbols();
    
    if (PIN_Init(argc, argv))
    {
        DpHelp();
        return EXIT_FAILURE;
    }

    if (!DppInit())
    {
        DpFinish(EXIT_FAILURE, NULL);
        return EXIT_FAILURE;
    }

    DLOG("Allocating tls key");

    DpTlsKey = PIN_CreateThreadDataKey(NULL);

    if (DpTlsKey == -1)
    {
        DLOG("Tls key not allocated");
        DpFinish(EXIT_FAILURE, NULL);
        return EXIT_FAILURE;
    }

    IMG_AddInstrumentFunction(DpImageLoaded, NULL);

    PIN_AddThreadStartFunction(DpThreadStartHandler, NULL);

    PIN_AddThreadFiniFunction(DpThreadFinishHandler, NULL);

    if (DppReducedCapture)
    {
        DLOG("Reduced capture enabled");
        TRACE_AddInstrumentFunction(DpTraceHandler, NULL);
    }
    else
    {
        DLOG("Instruction level capture enabled");
        INS_AddInstrumentFunction(DpInstructionHandler, NULL);
    }

    PIN_AddFiniFunction(DpFinish, NULL);

    PIN_StartProgram();
    
    return 0;
}
