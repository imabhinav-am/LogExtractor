import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Extractor {


    public static int check_time(int index, MappedByteBuffer buffer, Date fromTime){
        boolean flag = false;
        //System.out.println(index);
        StringBuilder str = new StringBuilder();
        for(int i=index; i<buffer.limit(); i++){
            byte read = buffer.get(i);
            char res = (char)read;
            //System.out.println(res);
            if(flag)
                str.append(res);
            if(res == 'Z') {
                //Date date1 = new SimpleDateFormat("dd/MM/yyyy").parse(sDate1);
                //System.out.println(str);
                if(str.length()==27)
                    break;
            }
            if(res == '\n')
                flag = true;
        }
        //System.out.println("Found Time : " + str);
        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(str);
        Instant it = Instant.from(ta);
        Date foundDateTime = Date.from(it);
        return foundDateTime.compareTo(fromTime);
    }

    public static int binary_search(int lo,int hi,MappedByteBuffer buffer,Date fromTime) {
        int mid;
        while (lo < hi) {
            //System.out.println("Low : " + lo + " High : " + hi);
            mid = lo + (hi - lo) / 2;
            if (check_time(mid, buffer, fromTime) >= 0)
                hi = mid;
            else
                lo = mid + 1;
        }
        return lo-1;
    }

    public static void main(String[] args) throws IOException {

        long startTime = System.nanoTime();
        //long ctr = 0;

        try { //LogFile-000001.log
            RandomAccessFile aFile = new RandomAccessFile("logs/LogFile-000001.log", "r");
            FileChannel inChannel = aFile.getChannel();
            MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
            StringBuilder str = new StringBuilder();
            boolean flag = true;
//            String fromTime = "2020-06-25T10:53:36.880995Z";
//            String toTime = "2020-06-25T10:53:36.882990Z";
            String fromTime = "2020-06-21T10:08:16.818762Z";
            String toTime = "2020-06-21T10:55:32.007485Z";
            //Date fromDateTime=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ").parse(fromTime);
            TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(fromTime);
            Instant it = Instant.from(ta);
            Date fromDateTime = Date.from(it);
            //System.out.println(fromDateTime);
            int startIndex = binary_search(0,buffer.limit(),buffer,fromDateTime);
            System.out.println("Start Index : " + startIndex);
            for (int i = startIndex; i < buffer.limit(); i++) {
                byte read = buffer.get(i);
                char res = (char)read;
                System.out.print(res);
                if(flag)
                    str.append(res);
                if(res == 'Z') {
                    //Date date1 = new SimpleDateFormat("dd/MM/yyyy").parse(sDate1);
                    //System.out.println(str);
                    if(toTime.equals(str.toString())) {
                        //System.out.println("Equal!!!!!");
                        i++;
                        while(res!='\n') {
                            read = buffer.get(i);
                            res = (char) read;
                            System.out.print(res);
                            i++;
                        }
                        break;
                    }
                    str.setLength(0);
                    flag = false;
                }
                if(res == '\n')
                    flag = true;
            }
            aFile.close();

        } catch (IOException ignored) {

        }

        long endTime = System.nanoTime();
        long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
        //System.out.println("Count : "+ ctr );
        System.out.println("Total elapsed time: " + elapsedTimeInMillis + " ms");

//        long startTime = System.nanoTime();
//
//        try {
//            RandomAccessFile aFile = new RandomAccessFile("test.log", "r");
//            FileChannel inChannel = aFile.getChannel();
//            MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
//
//            buffer.load();
//
//            CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(buffer);
//            String read = charBuffer.toString();
//
//            //System.out.println(read);
//
//            buffer.clear(); // do something with the data and clear or compact it.
//            inChannel.close();
//            aFile.close();
//
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }
//
//        long endTime = System.nanoTime();
//        long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
//        System.out.println("Total elapsed time: " + elapsedTimeInMillis + " ms");



//        long startTime = System.nanoTime();
//
//        File filePath = new File("test.log");
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
//
//        reader.lines().forEach(line -> {
//            // process liness
//            //System.out.println(line);
//        });
//
//        long endTime = System.nanoTime();
//        long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
//        System.out.println("Total elapsed time: " + elapsedTimeInMillis + " ms"  );



//        long startTime = System.nanoTime();
//        long ctr = 0;
//        try (Scanner sc = new Scanner(new File("test.log"), "UTF-8")) {
//            long freeMemoryBefore = Runtime.getRuntime().freeMemory();
//            System.out.println("Free Memory Before : " + freeMemoryBefore);
//            while (sc.hasNextLine()) {
//
//                String line = sc.nextLine();
//                // parse line
//                ctr++;
//                //System.out.println(line);
//            }
//
//            // note that Scanner suppresses exceptions
//            if (sc.ioException() != null) {
//                sc.ioException().printStackTrace();
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        long endTime = System.nanoTime();
//        long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
//        System.out.println("Count : "+ctr);
//        System.out.println("Total elapsed time: " + elapsedTimeInMillis + " ms"  );

    }
}