all copy from https://github.com/Sentaroh/SMBSync2  branch-2.55

build env
	win10 x64
	Android Studio Electric Eel | 2022.1.1 Patch 2
	（ Build -> Build Bundle(s)/Apk(s) -> Build Apk(s）)


modify list
	(1) move WrapperForSlf4j-1.0.2.jar (could find here: https://github.com/Sentaroh ) source files into project (package com.sentaroh.slf4j)
	(2) move JcifsFile-1.0.9.jar (could find here: https://github.com/Sentaroh ) source files into project (package com.sentaroh.jcifs, remove some smb1 code)
	(3) upgrade jcifs-ng,  slf4j-api,  bcprov-jdk15on
	(4) use another project Utilities_modify to build Utilities-1.0.18.aar