package processController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * 指令操作集合
 */
public class instFunctions {

    /**
     * 创建进程
     * @param name     进程名称
     * @param priority 进程优先级
     * @return
     */
    Process create(String name, int priority) {
        Process p = new Process();
        p.pName = name;
        p.priority = priority;
        p.status = 0; // 新创建的进程初始化为就绪态
        return p;
    }

    /**
     * 申请资源
     * @param type 资源类型
     * @param num  申请数量
     */
    boolean request(int type, int num) {
        if (GlobalVariable.totalRe[type] >= num) {
            //申请成功就将全局资源变量进行修改
            GlobalVariable.totalRe[type] = GlobalVariable.totalRe[type] - num;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 时间片轮转
     * @param rp 当前执行进程
     * @param readyList 就绪队列
     * @return
     */
    Process roundRobin(Process rp, ArrayList<Process> readyList) {
        //切换进程，将换下来的进程放到就绪队列末尾，然后按时间+优先级排序
        if (rp != null) { //如果不为空就轮转，如果为空，由于CPU资源不会空闲，除非已经没有进程需要使用，故不进行轮转
            rp.status = 0;
            readyList.add(rp);
            Collections.sort(readyList);

            rp = readyList.remove(0); //轮转
            rp.status = 1;
        }

        return rp;
    }

    /**
     * 释放资源
     *
     * @param type 释放资源类型
     * @param num  释放数量
     * @param rp   当前运行的进程
     */
    void release(int type, int num, Process rp, ArrayList<Process> readyList, ArrayList<Process> blockList) {
        //释放一个进程需要对全局变量resource修改，并且修改等待该资源的进程PCB，修改他们的状态以及所在的链表
        rp.resources[type] -= num;
        GlobalVariable.totalRe[type] += num;

        wakeUp(readyList, blockList); //释放资源可能会唤醒被阻塞的进程

    }

    /**
     * 销毁进程
     * @param rp 当前运行进程
     * @param name 被删除进程名称
     * @param readyList 就绪队列
     * @param blockList 阻塞队列
     */
    void destroy(Process rp, String name, ArrayList<Process> readyList, ArrayList<Process> blockList) {
        //销毁进程会释放资源，释放资源会导致阻塞进程被唤醒
        Process x = null;
        ArrayList<Process> recoList = new ArrayList<>();//记录需要的删除结点，最后统一删除

        x = findP(name, rp, readyList, blockList); //找到给定名称的进程，用临时变量x记录
        recoDel(x, recoList); //将要被删除的所有进程记录到recoList
        for (Process p : recoList) {
            if (p.status == -1) {
                blockList.remove(p);
            } else if (p.status == 0) {
                readyList.remove(p);
            } else if (p.status == 1) {
                GlobalVariable.flag = 1; //如果执行态结点也在删除队列，那么先将全局变量flag置1，作为标记
            }
            p.status = -2; //删除后将进程状态置为-2
         if (p.parent != null) {
                p.parent.childrenList.remove(p); //删除被删除进程的孩子结点
            }
            totalRel(p); //删除会释放所有的资源，此方法为释放所有资源
        }

    }

    /**
     * 删除会释放p的所有资源
     *
     * @param p
     */
    void totalRel(Process p) {
        for (int i = 1; i < 5; i++) {
            GlobalVariable.totalRe[i] += p.resources[i];
            p.resources[i] = 0;
        }
    }

    /**
     * 将需要删除的结点存入recoList中
     *
     * @param p
     * @param recoList
     */
    void recoDel(Process p, ArrayList<Process> recoList) {
        recoList.add(p);
        for (Process px : p.childrenList) {
            recoDel(px, recoList);
        }
    }

    /**
     * 寻找被删除结点的位置
     *
     * @param name
     * @param rp
     * @param readyList
     * @param blockList
     * @return
     */
    Process findP(String name, Process rp, ArrayList<Process> readyList, ArrayList<Process> blockList) {
        if (rp.pName.equals(name))
            return rp;

        for (Process p : readyList) {
            if (p.pName.equals(name))
                return p;
        }

        for (Process p : blockList) {
            if (p.pName.equals(name))
                return p;
        }

        return rp;
    }

    /**
     * 从阻塞队列中唤醒进程到就绪队列
     *
     * @param readyList
     * @param blockList
     */
    void wakeUp(ArrayList<Process> readyList, ArrayList<Process> blockList) {
        Iterator<Process> iterator = blockList.iterator();
        while (iterator.hasNext()) {
            Process bp = iterator.next();
            // 判断资源是否足够（注：resource[0]记录缺少哪一类型的资源，resource[5]记录需要多少该资源才能唤醒）
            if (GlobalVariable.totalRe[bp.resources[0]] >= bp.resources[5]) {
                GlobalVariable.totalRe[bp.resources[0]] -= bp.resources[5]; //唤醒前对全局资源做修改
                bp.resources[bp.resources[0]] += bp.resources[5]; //被唤醒进程所占有的bp.resource[0]类型的资源数目
                readyList.add(bp);
                bp.status = 0;
                Collections.sort(readyList);
                iterator.remove(); //唤醒到就绪列表后从阻塞列表中删除
            } else {
                break;
            }
        }
    }
}


//    /**
//     * 销毁进程
//     * @param name 被销毁进程名称
//     */
//    void destroy(Process rp, String name, ArrayList<Process> readyList, ArrayList<Process> blockList){
//        //销毁进程会释放资源，释放资源会导致阻塞进程被唤醒
//        Process x = new Process();
//        x = findP(name, rp, readyList, blockList);
//        while(x.status != -2) { //如果指定进程还未被删除的话就不断调用，直至删除（因为delLeaves()方法只删除叶节点，所以一次可能删除不完）
//            delLeaves(x, readyList, blockList);
//        }
//    }

//    /**
//     * 寻找被删除子树的叶节点,从叶节点开始删除进程、释放资源
//     * @param p 进程
//     * @return
//     */
//    int delLeaves(Process p, ArrayList<Process> readyList, ArrayList<Process> blockList){
//        if(p.childrenList.isEmpty()){
//            //删除两个队列中的p，通过比较进程状态来确定进程在什么位置，从而删除
//            if(p.status == -1){ //p在阻塞队列中
//                for(int i = 1; i < 5; i++){
//                    GlobalVariable.totalRe[i] += p.resources[i];
//                    p.resources[i] = 0;
//                }
//                blockList.remove(p);
//                //释放内存空间
//                p.status = -2; //被删除
//                p = null;
//
//            }else if(p.status == 0){ //p在就绪队列中
//                for(int i = 1; i < 5; i++){
//                    GlobalVariable.totalRe[i] += p.resources[i];
//                    p.resources[i] = 0;
//                }
//                readyList.remove(p);
//                //释放内存空间
//                p.status = -2; //被删除
//                p = null;
//
//            }else if(p.status == 1){ //p是执行状态的进程
//                for(int i = 1; i < 5; i++){
//                    GlobalVariable.totalRe[i] += p.resources[i]; //资源释放
//                    p.resources[i] = 0;
//                }
//                //释放内存空间
//                p.status = -2; //被删除
//                p = null;
//
//                GlobalVariable.flag = 1;
//            }
//        }else{
//            Iterator<Process> iterator = p.childrenList.iterator();
//            while (iterator.hasNext()) {
//                Process tmp = iterator.next();
//                delLeaves(tmp, readyList, blockList); //删除状态表中的结点
//                iterator.remove(); // 删除孩子链表中的结点， 与上面结合才能同步
//                if(tmp.status == -1){
//                    blockList.remove(tmp);
//                }else if(tmp.status == 0){
//                    readyList.remove(tmp);
//                }else if(tmp.status == 1){
//                    GlobalVariable.flag = 1;
//                }
//            }
//        }
//
//        return GlobalVariable.flag;
//    }