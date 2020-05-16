# Dragon Dance

## What is that?

Dragon Dance is a plugin for [Ghidra](https://github.com/NationalSecurityAgency/ghidra) to get visualize and manipulate the binary code coverage data. Coverage data can be imported from the multiple coverage sources. For now the plugin supports Dynamorio and Intel Pin binary instrumentation tools.  Dynamorio has its own coverage collection module called "drcov". Intel Pin does not provide a builtin coverage collector module. To handle the lack of module situation I have to write my own coverage collection module for Intel Pin. So I wrote a coverage collection module for Intel Pin named **ddph** (Dragon Dance Pin Helper). So you can use that. You can view ddph's source from this [link](https://github.com/0ffffffffh/dragondance/tree/master/coveragetools). If you are lazy to compile for your own, you can use the compiled binaries I provided for Windows, macOS and Linux.

![Dragon Dance Main](https://user-images.githubusercontent.com/437161/57895521-b9205280-7854-11e9-83ef-b9f09101efea.gif)

Dragon Dance can import and use multiple coverage data in the same session. (Also it supports multi-session but for now that's not usable by the GUI). And you can switch between them or apply intersection, difference, distinct or sum operation with each other quickly.

Dragon Dance lets you to view intensity of the executed instructions.  So you can get hint on which instructions how often executed. Also you can view the coverage visualization on the function graph window.

![](https://user-images.githubusercontent.com/437161/57895536-ca695f00-7854-11e9-90ae-a3703ecb34ce.png) 

## Scripting

Dragon Dance also supports its own scripting system. 

![](https://user-images.githubusercontent.com/437161/57895545-d81ee480-7854-11e9-8713-b18036ff0b80.gif)

It lets you flexible way to play with the coverage data. You can load, delete, show, intersect, diff, distinct, sum operation on them. Following section will be contained the scripting system and api. Press Alt + Enter keys to execute the script.

#### Built-in Functions

Built-in functions are implementation of the internal coverage operations to supply an interface to the scripting system. Built-in function can be return coverage object variable or nothing. Built-ins may have aliases.

They are accepts Built-in Arg as a parameter. Parameters can be variable length.

###### Built-in Arg

Built-in Arg is a reference to hold different type of the value. Built-in Args passed left to right order. Built-in Arg can hold following value types:

- Coverage Data Object (which is returned by Built-ins)
- String
- Integer (Can be decimal, hexadecimal or octal forms)

#### Variables

Variables are responsible to hold the coverage object only. They can be loaded by built-in functions. They can be passed as an parameter (**Built-in Arg**) to the Built-in Functions.

There is two types of the coverage object. 

*Physical Coverage Object* and *Logical Coverage Object*

**Physical Coverage Object** points a coverage object that it loaded directly from the coverage file. They are visible on the coverage table which is on the GUI. So you can interact them via the GUI operations.

**Logical Coverage Object** points a coverage object that has processed in a built-in function and returned from it as a result. They are not visible on the GUI but they can lives in a Variable until they are destroyed. 

Coverage object maintained by the Variable object automatically for both of type of the coverage object. For example;

```
cov1 = load("firstcoverage.out")
cov2 = load("secondcov.out")

cov1 = diff(cov1,cov2)
```

In this example cov1 and cov2 are variable. And both variables has physical coverage object. diff built-in takes both variables and sets the return value to the cov1 variable. That overwrite operation will set the result coverage object to the variable but does not delete the coverage object because that is a physical coverage object. That coverage data will remain in the session and also GUI table.

Let's think previous example like this;

```
cov1 = load("first.out")
cov2 = load("second.out")
cov3 = load("third.out")
rvar = sum(cov1,cov2,cov3)
rvar = diff(rvar, cov2)
```

In this example three physical coverage variable goes into sum operation and the sum operation returns logical result coverage object. Then the diff operation takes a logical and a physical variable in it and overwrites the variable named rvar.

In this case, the result will be set to the rvar and it's previous coverage value destroyed immediately. Because this was a logical object and should be deleted to prevent object leakage. If you want to destroy a variable that it contained a physical coverage object, you have to call `discard` built-in to do. All built-ins will be detailed below.

You can write complex scripts using nested built-in calls, you can write something like so:

`cres = diff(intersect(a, load("another.log"), c, d), sum(e,f) )`

you don't have to write the logic line by line.

### Built-in References

The following API documentations and their behaviors may change until reached final version. 

**clear()**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | None                                                         |
| Minimum Parameter Count | 0                                                            |
| Maximum Parameter Count | 0                                                            |
| Description             | This built-in clears the being visualized coverage and sets the active coverage to null. |
| Aliases                 | None                                                         |



**cwd(** *String* : workingDirectory **)**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | None                                                         |
| Minimum Parameter Count | 1                                                            |
| Maximum Parameter Count | 1                                                            |
| Description             | Sets the current working directory with the given path. All import calls without absolute path after the **cwd** the coverage files will be searched in the active working directory. |
| Aliases                 | None                                                         |



**diff(** *Variable* : var1, var2, ..... var*N* **)**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | Variable                                                     |
| Minimum Parameter Count | 2                                                            |
| Maximum Parameter Count | Unlimited                                                    |
| Description             | Applies difference operation to given variable length Variables. And returns the result coverage variable. |
| Aliases                 | None                                                         |



**discard(** *Variable* : var1, var2, ..... var*N* **)**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | None                                                         |
| Minimum Parameter Count | 1                                                            |
| Maximum Parameter Count | Unlimited                                                    |
| Description             | Destroys variables whatever physical or logical. It will destroy the coverage object first and then unregisters the variable name from the variable list. After this call whole given variables becomes undefined. |
| Aliases                 | del                                                          |



**distinct(** Variable : var1, var2, ..... var*N* **)**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | Variable                                                     |
| Minimum Parameter Count | 2                                                            |
| Maximum Parameter Count | Unlimited                                                    |
| Description             | Applies distinct (xor) operation to given variable length variables. And returns the result coverage variable |
| Aliases                 | xor                                                          |



**goto(** *Integer* : offset **)**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | None                                                         |
| Minimum Parameter Count | 1                                                            |
| Maximum Parameter Count | 1                                                            |
| Description             | Locates the current address selection by given offset. Real address value calculated by adding the offset value to the image base value. |
| Aliases                 | None                                                         |



**import(** *String* : filePathOrCoverageName **)**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | Variable                                                     |
| Minimum Parameter Count | 1                                                            |
| Maximum Parameter Count | 1                                                            |
| Description             | Imports coverage data from the physical coverage file. Takes relative or absolute path. Or coverage name that it loaded physically earlier. If given path is an absolute path, import loads directly from the path. Otherwise it looks under the current working directory to load. In both cases, import will check that the coverage data already loaded or not using its path. If loaded already it returns cached coverage variable. Or if given value is a name of a physical coverage, it lookups the coverage map from its session and returns coverage object if exists. |
| Aliases                 | get, load                                                    |



**intersect(** *Variable* : var1, var2, ..... var*N* **)**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | Variable                                                     |
| Minimum Parameter Count | 2                                                            |
| Maximum Parameter Count | Unlimited                                                    |
| Description             | Applies intersection operation to the given variable length variables. And returns the result coverage variable |
| Aliases                 | and                                                          |



**show(** *Variable* : var **)**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | None                                                         |
| Minimum Parameter Count | 1                                                            |
| Maximum Parameter Count | 1                                                            |
| Description             | Visualizes given coverage variable. If there is an actively visualized coverage object and that is a logical, the function destroys previously coverage object immediately and shows given one. |
| Aliases                 | None                                                         |



**sum(** *Variable* : var1, var2, ..... var*N* **)**

| Property                | Description                                                  |
| ----------------------- | ------------------------------------------------------------ |
| Return Value            | Variable                                                     |
| Minimum Parameter Count | 1                                                            |
| Maximum Parameter Count | Unlimited                                                    |
| Description             | Applies sum operation to the given variable length variables. And returns the result coverage variable. |
| Aliases                 | or, union                                                    |

**Fix Ups**

Dragon Dance can try to fix a misanalysed situation in ghidra during import the coverage data file. On some binaries, the ghidra does not decompile instructions of a function due to unexpected code generation by the compiler. Dragon Dance checks loaded image and the coverage data integrity. if they are valid for each other and the address is belongs to a executable section but there is lack of instruction decompilation, The plugin asks to fix. Then it tries to fix via decompiling the raw section.

![](https://user-images.githubusercontent.com/437161/57895491-99892a00-7854-11e9-98b6-af9a13653a55.gif)

In the future versions of the plugin, it may contains more fixups or workarounds for the image.

### Installation

Installation is quite easy.

Start Ghidra.

Click "File" menu and then select "Install Extensions.."

Click the Green Plus icon from the Top-Right of the window

Select plugin zip package and select Ok.

Select dragondance from the list

Click Ok and restart the ghidra

### Launching DragonDance

During the first loading of a binary to the Ghidra after the plugin installation, Ghidra should ask you whether you want to configure the newly installed plugin or not.

If you click the Yes button, DragonDance plugin should appear immediately. 

If you click the No button you have to activate manually yourself.

In order to active manually,


Click "File" menu and then select Configure from the Disassembly Window (CodeBrowser)

Click the little plug icon from the top right corner of the Configure Tool window.

Find the DragonDance item from the plugin list and enable it's checkbox then click Ok

Dragon Dance window should be appeared.

After the activation you should able to see Dragon Dance item in the Window menu.


### Collecting Coverage Data

As I described before, Dragon Dance can import coverage data from Dynamorio and the Intel Pin. (For now). Actually these are generic binary instrumentation tools. You have to use proper module with them to collect coverage data. Dynamorio has it's own coverage module called drcov. You can use that built-in module to collect coverage.

**Using Dynamorio**

You can collect coverage data from the Dynamorio using following command:

`drrun -t drcov -logdir [COVERAGE_OUTPUT_DIRECTORY_PATH] -- [EXECUTABLE_PATH_TO_EXAMINE] [EXECUTABLE_ARGUMENTS]`

The output will be placed into given directory as *drcov.[EXECUTABLE_NAME].[ID].proc.log* format. 

**Using Intel Pin**

As I mention before, Intel Pin does not provide any built-in coverage collector module. You have to use a custom pin module. Fortunally I crafted my own to collect coverage from the Pin. So that brings a few advantage to us. I can extend it when needed or add extra features, options in it. 

While later versions may work, only Intel PIN 3.7 is supported. These are not immediately offered on the Intel PIN page, so here are direct download links:

https://software.intel.com/sites/landingpage/pintool/downloads/pin-3.7-97619-g0d0c92f4f-gcc-linux.tar.gz

https://software.intel.com/sites/landingpage/pintool/downloads/pin-3.7-97619-g0d0c92f4f-msvc-windows.zip

https://software.intel.com/sites/landingpage/pintool/downloads/pin-3.7-97619-g0d0c92f4f-clang-mac.tar.gz


You can access **ddph**'s source [here](https://github.com/0ffffffffh/dragondance/tree/master/coveragetools). I will share binaries for Windows, macOS and Linux. Or you can build your very own binary using it's build shell script.

To be collect coverage data from Intel Pin use following command:

`pin -t ddph.[so,dylib,dll] [ddph options] -- [EXECUTABLE_PATH_TO_EXAMINE] [EXECUTABLE_ARGUMENTS]`

ddph has some options for collection.

-o: with this option you can specify coverage output filename. (*default:* **ddph.out**)

-l: you can specify operation log filename. if you pass "no" to this option, ddph will not perform logging operation. (*default:* **ddph.log**)

-p: capture detail level. this option can be **reduced** or **high**. high level captures whole instructions one by one and builds pre-process execution blocks. This will gives you more intensive coverage output but that's slower than reduced. reduced level uses pin's trace blocks so that is way more faster than high level. But that will not cause huge differences on the different level of coverage output. If you don't want to do specific things, consider using reduced level.  (*default:* **reduced**) 

**For macOs users:** <u>Starting with macOS 10.11 (OS X El Capitan), the os comes with a security layer named **System Integrity Protection** *SIP*. That prevents the user mode processes that are tries to do injection or modification upon another process even if you are running under root privileges.</u>

To overcome this prevention, you have to disable it. To do that follow these steps below.

Restart macOS

During the boot process, press Command + R keys and keep pressed.

The OS eventually enters into the Recovery Mode

Open a terminal from the Utilities section

type `csrutil status` and enter. You must see that the SIP enabled.

type `csrutil disable` and press enter. 

type `csrutil status` again to make sure it is disabled or not. Then restart the OS and let it boot as normal. Now you are ready to use binary instrumentation tools.



![](https://user-images.githubusercontent.com/437161/57895609-2f24b980-7855-11e9-8943-3a903ee59139.gif)

![](https://user-images.githubusercontent.com/437161/57895596-161c0880-7855-11e9-9bb4-027034ca2069.gif)

![](https://user-images.githubusercontent.com/437161/57895623-3f3c9900-7855-11e9-8b21-cc0d03adb3d7.gif)

#### Build instructions

First, download ghidra (latest version, currently 9.1.2) and dragondance
```
$ wget https://ghidra-sre.org/ghidra_9.1.2_PUBLIC_20200212.zip
$ wget https://github.com/0ffffffffh/dragondance/archive/master.zip
$ unzip ghidra_9.1.2_PUBLIC_20200212.zip
$ unzip master.zip
```
 Next install gradle and jdk
```
$ sudo apt install openjdk-11-jdk
$ wget https://services.gradle.org/distributions/gradle-5.2.1-bin.zip
$ sudo unzip -d /opt/gradle gradle-5.2.1-bin.zip
```
create new profile file
```
$ sudo vi /etc/profile.d/gradle.sh
```
and add the following to add gradle to the PATH in every subsequent login.
```
export GRADLE_HOME=/opt/gradle/gradle-5.2.1
export PATH=${GRADLE_HOME}/bin:${PATH}
```
to do it immediately, without having to logout
```
$ source /etc/profile.d/gradle.sh
```
we can now proceed to build dragondance
```
$ cd dragondance-master/
$ gradle -PGHIDRA_INSTALL_DIR=/home/ubuntu/ghidra_9.1.2_PUBLIC
> Task :buildExtension

Created ghidra_9.1.2_PUBLIC_20200506_dragondance-master.zip in /home/ubuntu/dragondance-master/dist

BUILD SUCCESSFUL in 29s
5 actionable tasks: 5 executed
ubuntu@ubuntu:~/dragondance-master$ 
```
where you may have to adjust the path to where you downloaded ghidra to. You can now find the built extension in `dragondance-master/dist`

#### The Features that I want to bring on it

Command prompt style line by line script execution.

Context change awareness

Function (Routine) based coverage visualization

Execution flow awareness

More built-ins to the scripting

Its own coverage database format to save and load faster and keep latest changes on the session.

Pseudo code painting. (Ghidra does not provide an API for this. So I have to study on the Ghidra's source code to find out a way or workaround to achieve it. ) 

UI enhancements 

##### Project Author

Oğuz Kartal ([@0ffffffffh](https://twitter.com/0ffffffffh))

