package processController;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class TestShell {
    public static void main(String[] args) {
//            Scanner input = new Scanner(System.in);
//            System.out.println("请输入指令文件路径：");
//            String filePath = input.next(); //input.txt
            String strLine = null; //读取的一条指令
            String[] parts = null; //分割后的结果
            String inst = null; // 指令类型，有cr,del,req,rel,to等
            File file = new File(args[0]);
            instFunctions instFunc = new instFunctions();// 方法集
            Process rp = null; // 当前正在执行的CPU
            Process rpd = new Process(); // 通过时间片轮转被换下来，或者被优先级高的进程抢占的进程
            ArrayList<Process> readyList = new ArrayList<>();
            ArrayList<Process> blockList = new ArrayList<>();

            {
                if (rp == null) {
                    System.out.print("init ");
                }
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    while ((strLine = reader.readLine()) != null) {
                        parts = strLine.split(" "); // 两个反斜杠是转义
                        inst = parts[0]; //指令类型

                        switch (inst) {
                            case "cr":
                                String pName = parts[1];
                                int priority = Integer.valueOf(parts[2]); // int->String : String.valueof
                                Process p = null;
                                p = instFunc.create(pName, priority);
                                p.parent = rp;
                                readyList.add(p);
                                Collections.sort(readyList);
                                if (rp == null) { //开始判定是不是根进程
                                    //p.parent = null; //父节点为空表示根进程
                                    rp = readyList.remove(0);
                                    rp.status = 1;
                                } else {
                                    rp.childrenList.add(p); //创建进程为当前运行进程的子进程

                                    if (p.priority > rp.priority) {//创建后比较优先级，若优先级高于当前运行进程的优先级，则抢占cpu，当前进程放入就绪队列进行排序
                                        rp.status = 0;
                                        rpd = rp; // 原本在CPU上的进程被换下来
                                        readyList.add(rpd);
                                        Collections.sort(readyList);//按优先级排序
                                        p.status = 1; //状态改为执行态
                                        rp = p; //当前运行进程改为p，即抢占进程
                                        readyList.remove(rp);
                                    }
                                }

                                break;
                            case "req":
                                //request(type, num);
                                int type = Integer.valueOf(parts[1].charAt(1) - 48);
                                int num = Integer.valueOf(parts[2]);

                                if (instFunc.request(type, num)) {
                                    //如果申请成功，则对当前运行进程做操作
                                    rp.resources[type] += num;
                                } else {
                                    rp.resources[0] = type; //记录当前进程因哪种资源被阻塞
                                    rp.resources[5] = num; //记录当前进程所需要改资源的数目
                                    rp.status = -1; //阻塞态
                                    rpd = rp;
                                    blockList.add(rpd);
                                    //Collections.sort(blockList);
                                    //下一个就绪进程使用CPU
                                    if (!readyList.isEmpty()) {
                                        rp = readyList.remove(0);
                                        rp.status = 1;
                                    }
                                }

                                break;
                            case "to":
                                rp = instFunc.roundRobin(rp, readyList);

                                break;
                            case "de":
                                String name = parts[1];
                                GlobalVariable.flag = -1;
                                instFunc.destroy(rp, name, readyList, blockList);
                                //删除会释放资源，从而唤醒进程
                                instFunc.wakeUp(readyList, blockList);

                                if (GlobalVariable.flag == 1) { //如果返回值是1，则说明是当前运行结点也被删除，需要重新挑选进程占用CPU
                                    rp.status = -2;
                                    rp = null;
                                    if (!readyList.isEmpty()) {
                                        rp = readyList.remove(0);
                                        rp.status = 1;
                                    }
                                }

                                break;
                            case "rel":
                                type = Integer.valueOf(parts[1].charAt(1) - 48);
                                num = Integer.valueOf(parts[2]);
                                instFunc.release(type, num, rp, readyList, blockList);
                                //释放资源会唤醒进程，从而可能导致抢占CPU
                                if (readyList.get(0).priority > rp.priority) {
                                    rp.status = 0;
                                    readyList.add(rp);
                                    Collections.sort(readyList);
                                    rp = readyList.remove(0);
                                    rp.status = 1;
                                }

                                break;
                            case "lp":
                                //所有进程和它的状态
                                System.out.println();
                                System.out.println();
                                System.out.println("所有进程及其状态如下：(1表示运行态，0表示就绪态，-1表示阻塞态)");
                                System.out.println("pName" + "\t" + "status");
                                if(rp != null){
                                    System.out.println(rp.pName + "\t\t" + rp.status);
                                }
                                for(Process pro : readyList){
                                    System.out.println(pro.pName + "\t\t" + pro.status);
                                }
                                for(Process pro : blockList){
                                    System.out.println(pro.pName + "\t\t" + pro.status);
                                }
                                break;
                            case "lr":
                                //所有资源和它的状态
                                System.out.println();
                                System.out.println("所有资源及其状态如下：");
                                System.out.println("type" + "\t" + "total" + "\t" + "available");
                                for(int i = 1; i < 5; i++){
                                    System.out.println("R" + i + "\t\t" + i + "\t\t" + GlobalVariable.totalRe[i]);
                                }
                                break;
                            default:
                                System.out.println("指令错误！\n");
                        }
                        if(!(inst.equals("lp") || inst.equals("lr"))) {
                            if (rp != null) {
                                System.out.print(rp.pName + " ");
                            } else {
                                System.out.print("init ");
                            }
                        }

                    }
                    //一个文件执行完需要把全局变量恢复原状
                    for (int i = 0; i < 5; i++) {
                        GlobalVariable.totalRe[i] = i;
                    }
//                    System.out.println();
//                    System.out.println();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
//}
