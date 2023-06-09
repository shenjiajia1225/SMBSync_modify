## 1.功能  
SMBSync2是一款通过无线局域网使用SMB1、SMB2或SMB3协议在Android设备内部存储、SDCARD/USB-OTG和PC/NAS之间进行文件同步的工具。 同步从主站到目标站是单向的，可以进行镜像、移动、复制和存档。 (内部存储、SDCARD、USB-OTG SMB和ZIP的组合也是可以的。)  
定期同步可以由SMBSync2的调度功能或外部应用程序（如Tasker或AutoMagic）启动。  

- 镜像  
  将主站侧的目录和文件差额拷贝（*1）到目标站侧，复制完成后，从目标站侧删除主站侧不存在的文件和目录。  
- 移动  
  将主控端目录和文件差额复制到目标端，删除主控端被复制到目标端的文件。 但是，如果主文件和目标文件的名称、文件大小和修改日期相同，则主文件会被删除，而不会被复制。  
- 复制  
  差分将主目录中的文件复制到目标目录中。  
- 封存  
  如果主目录中的照片和视频是在归档执行日期前7天或30天之前拍摄的，则将其移动到目标中。但是，您不能使用zip来锁定目标。   
以下文件类型有资格进行归档。   
gif、"jpg"、"jpeg"、"jpe"、"png"、"mp4"、"mov"。  

*1 当满足以下三个条件中的任何一个时，该文件将被判定为差异文件，并将被复制或移动。 但是，文件大小和最后修改时间可以被同步任务的选项忽略。  

1. 该文件不存在。  
2. 不同的文件大小。  
3. 最后修改的日期和时间相差3秒以上（秒数可以通过同步任务中的选项更改）。  

在高级选项中，有很多比较设置可以调整，（下面是一个例子  
- 时间容许间隔可以设置为忽略小于1、3、5或10秒的差异，以便与FAT/exFAT介质兼容。  
- 支持忽略夏令时。  
- 如果目标文件比主文件新或尺寸较大，可选择不覆盖。  

## 2.Documents  
[FAQ](https://sentaroh.github.io/Documents/SMBSync2/SMBSync2_FAQ_EN.htm)  
[Manual](https://sentaroh.github.io/Documents/SMBSync2/SMBSync2_Desc_EN.htm)  
