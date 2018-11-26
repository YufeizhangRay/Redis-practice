package cn.zyf.redis.demo;


public class UnitTest extends Thread{

    @Override
    public void run() {
        while(true){
            DistributedLock distributedLock=new DistributedLock();
            String rs=distributedLock.acquireLock("updateOrder",
                    2000,5000);
            if(rs!=null){
                System.out.println(Thread.currentThread().getName()+"-> 成功获得锁:"+rs);
                try {
                    Thread.sleep(1000);
                    distributedLock.releaseLockWithLua("updateOrder",rs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public static void main(String[] args) {
        UnitTest unitTest=new UnitTest();
        for(int i=0;i<10;i++){
            new Thread(unitTest,"tName:"+i).start();
        }
    }
}
