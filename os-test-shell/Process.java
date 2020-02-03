package processController;

import java.util.ArrayList;

public class Process implements Comparable<Process> {
    /**
     * PCB控制模块
     */
    String pName; //进程名称
    int[] resources = {0, 0, 0, 0, 0, 0}; //共4种资源，数组0位置为进程被阻塞时所需资源的类型，数组5位置是唤醒该进程所需要的资源数目
    int status; //ready:0, block:-1, running:1
    int priority = -1; //进程优先级
    Process parent = null; //一个进程最多有一个父进程
    ArrayList<Process> childrenList = new ArrayList<>(); // 一个进程可能有多个子进程

    @Override
    public int compareTo(Process o) {
        return -(this.priority - o.priority);
    } //类排序，从大到小排序
}
