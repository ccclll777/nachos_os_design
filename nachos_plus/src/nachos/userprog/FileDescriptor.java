package nachos.userprog;

import nachos.machine.OpenFile;

//文件描述符类
public class FileDescriptor {
    private String fileName;//文件名
    private OpenFile file;//nachos 实现的文件类  实现文件的读写
    private boolean toRemove = false;//文件是否已经被进程读入

    public FileDescriptor()
    {
        this.fileName = "";
        this.file  = null;
    }
    public FileDescriptor(String fileName, OpenFile file) {
        this.fileName = fileName;
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public OpenFile getFile() {
        return file;
    }

    public void setFile(OpenFile file) {
        this.file = file;
    }

    public boolean isToRemove() {
        return toRemove;
    }

    public void setToRemove(boolean toRemove) {
        this.toRemove = toRemove;
    }
}
