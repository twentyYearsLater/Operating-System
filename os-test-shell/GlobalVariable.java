package processController;

public class GlobalVariable {
    static int[] totalRe = {0, 1, 2, 3, 4}; //数组0位置保留不用，totalRe[i]的值表示Ri资源的数量
    static int flag = -1; // 判断被删除的结点中是否有正在运行的结点，flag=1表示有，则需要其他进程使用CPU
}
