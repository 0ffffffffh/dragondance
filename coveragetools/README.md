## DDPH (Dragon Dance Pin Helper)

**ddph** is a module for Intel Pin that can collect and supply coverage data to the Dragon Dance plugin. You can find usage of this module on the Dragon Dance's readme section.

dpph comes with its build shell script for macOS, Linux. And comes with a Visual C++ project file for Windows to build with msbuild tool or directly from the Visual Studio.

If you don't want to waste your time with building it from the source, you can find the binaries for Windows, Linux and macOS.

### Building from the source

##### For Windows:

ddph comes with Visual C++ Project file and two props file which are contained macro variable definition for the project. That means you can build with Visual Studio or msbuild command line tool.

*Preparation:*

Before building the source you have to set **PIN_ROOT** environment variable with the Intel Pin dev kit's root directory. Otherwise compiler could not locate the required headers and the libraries.

Then you can load into Visual Studio via click the vcxproj file and build within GUI,  or you can use msbuild.

Building the ddph with the msbuild is not complicated. Open a command prompt. Set current directory to the ddph's source folder.

Then type following command

`msbuild ddph.vcxproj /p:Configuration=[BUILD_TYPE] /p:Platform=[ARCHITECTURE]`

*BUILD_TYPE* can be "Debug" or "Release"
*ARCHITECTURE* can be "x64" for 64 bit compilation, "Win32" for 32 bit compilation. That's all. 



![](https://user-images.githubusercontent.com/437161/57917575-a41be180-789d-11e9-9478-ca43eff54512.gif)



##### For Linux and macOS

ddph also comes with a build shell script (**build.sh**) for Linux and macOS. You can execute the script thats all.

*Preparation:*

Before execute the build script you have to set script variable with pin kit directory. Open build.sh and change PIN_ROOT variable with your valid intel pin dev kit root path.

Also you make sure that it has proper permission to execute. (chmod +x build.sh)

If everything is ok, simply execute the build.sh. build script can take arguments for build operation.

`./build.sh [ARCHITECTURE] [OPTIONAL_FLAGS]`

*ARCHITECTURE* can be "x32" for 32 bit or "x64" for 64 bit compilation. If you are running on 64 bit os but want to 32 bit compilation you have to aware to cross compilation thing. Build script checks system's arch and sets -m32 flag if OS arch is 64 bit and compilation arch is x32.

*OPTIONAL_FLAGS* this flag can be only **-oldabi**. If your C++ compiler does not met Pin dev kit's ABI , you may get in trouble with the compilation. The C++11 ABI changes breaks the ABI compatibility of the binary. If you are in such a situation, your compilation will be broken with an error that is says there is an ABI problem.  To handle that problem,  build script has a flag to force the compiler to use older ABI. To get more information about the ABI changes you can follow this [link](https://gcc.gnu.org/onlinedocs/libstdc++/manual/using_dual_abi.html)

![](https://user-images.githubusercontent.com/437161/57917571-a1b98780-789d-11e9-8c89-0c7feb6c0229.gif)

![](https://user-images.githubusercontent.com/437161/57917565-9ebe9700-789d-11e9-8a10-1dc6efc965af.gif)

You can get some warnings during the compilation but that's ok, these are noisy and ignorable.

