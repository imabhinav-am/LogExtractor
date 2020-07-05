import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class temp1 {

    public static Integer binary_search(int lo,int hi,MappedByteBuffer buffer,Date fromTime) {
        int mid=0;
        while (lo < hi) {
            mid = lo + (hi - lo) / 2;
            if (check_time(mid, buffer, fromTime) >= 0)
                hi = mid;
            else
                lo = mid + 1;
        }
        StringBuilder str = new StringBuilder();
        for(int i=lo; i<buffer.limit(); i++){
            byte read = buffer.get(i);
            char res = (char)read;
            str.append(res);
            if(res == 'Z') {
                if(str.length()==27)
                    break;
            }
            if(res=='\n') {
                return i+1;
            }
        }
        return lo;
    }

    public static Integer check_time(Integer index, MappedByteBuffer buffer, Date fromTime){
        StringBuilder str = new StringBuilder();
        for(Integer i=index; i<buffer.limit(); i++){
            byte read = buffer.get(i);
            char res = (char)read;
            str.append(res);
            if(res == 'Z') {
                if(str.length()==25)
                    break;
            }
            if(res=='\n') {
                str.setLength(0);
            }
        }

        Date foundDateTime = getDateTime(str.toString());
//        System.out.println(fromTime);
//        System.out.println(foundDateTime);
        return foundDateTime.compareTo(fromTime);
    }

    public static Date getDateTime(String str){
        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(str);
        Instant it = Instant.from(ta);
        return Date.from(it);
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.nanoTime();
        RandomAccessFile aFile = new RandomAccessFile("C:/Users/ABHI/IdeaProjects/LogExtractor/logs/LogFile-000001.log", "r");
        FileChannel inChannel = aFile.getChannel();
        long size = inChannel.size();
        System.out.println(size);
        long i = 2000000000, j = 0;
        String fromTime = null, toTime = null, path = null;
        fromTime = "2020-06-26T17:38:08.1016Z";
        toTime = "2020-06-26T17:38:08.1066Z";
        path = "C:\\Users\\ABHI\\IdeaProjects\\LogExtractor\\logs\\";
        MappedByteBuffer buffer;
        while(i<=size) {
            buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, j, 2000000000);
//            for (int x = 0; x < i-j; x++) {
//                byte read = buffer.get(x);
//                char res = (char)read;
//            }
            int chk = check_time(0,buffer,getDateTime(fromTime));
            if(chk==0 || chk==1){
                break;
            }
            j = i;
            if(i==size)
                break;
            if(size-i>2000000000){
                i += 2000000000;
            }
            else{
                i = size;
            }
        }
        long startIndex;
        if(i==size){
            startIndex = j;
            buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, j, 10000000);
        }
        else if(j!=0){
            //System.out.println("Here");
            long pos = j-2000000000;
            buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, pos, 2000000000);
            startIndex = binary_search(0,2000000000,buffer,getDateTime(fromTime));
            System.out.println(startIndex+j);
            buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, startIndex+pos, 100000000);
//            for(int y=0; y<buffer.limit(); y++){
//                System.out.print((char)buffer.get());
//            }
        }
        else{
            buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, 2000000000);
            startIndex = binary_search(0,2000000000,buffer,getDateTime(fromTime));
            buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, 10000000);
        }
        boolean flag = true;
        StringBuilder str = new StringBuilder();
        Date foundTime, toDateTime = getDateTime(toTime);
        System.out.println("Start Index : " + startIndex + " " + j);
        for(int x=0; x<buffer.limit(); x++){
            byte read = buffer.get(x);
            char res = (char)read;
            str.append(res);
            if(res == 'Z') {
                foundTime = getDateTime(str.toString());
                if(foundTime.compareTo(toDateTime) > 0) {
                    break;
                }
            }
            if(res == '\n') {
                System.out.print(str);
                str.setLength(0);
            }
        }
        long endTime = System.nanoTime();
        long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
        System.out.println("Total elapsed time: " + elapsedTimeInMillis + " ms"  );
    }
}
