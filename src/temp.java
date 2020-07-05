import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class temp {

    //java LogExtractor -f 2020-06-21T10:08:30.464396Z -t 2020-06-21T10:08:30.470382Z -i C:\Users\ABHI\IdeaProjects\LogExtractorJava\logs\
    //java LogExtractor -f 2020-06-21T10:11:17.734744Z -t 2020-06-21T10:50:57.203789Z -i C:\Users\ABHI\IdeaProjects\LogExtractorJava\logs\


    public static TreeSet<String> findFile(Date fromDateTime, Date toDateTime){
        TreeSet<String> hs = new TreeSet<>();
        String contents = null;
        try {
            contents = new String(Files.readAllBytes(Paths.get("C:\\Users\\ABHI\\IdeaProjects\\LogExtractorJava\\src\\FileMappings.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert contents != null;
        String[] lines = contents.split("\\r?\\n");
        int j=-1;
        for (int i=0; i<lines.length; i++) {
            //System.out.println(line);
            String[] ll = lines[i].split(" ");
            Date startTime = getDateTime(ll[1]);
            Date endTime = getDateTime(ll[2]);
            if(startTime.compareTo(fromDateTime)<=0 && endTime.compareTo(fromDateTime)>=0){
                hs.add(ll[0]);
                if(endTime.compareTo(toDateTime)<0 && endTime.compareTo(fromDateTime)>0){
                    j = i+1;
                    break;
                }
            }
        }
        if(j!=-1) hs.add(lines[j].split(" ")[0]);
        return hs;
    }

    public static Date getDateTime(String str){
        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(str);
        Instant it = Instant.from(ta);
        return Date.from(it);
    }
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
        Date foundDateTime = getDateTime(str.toString());
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
        return lo;
    }

    public static void main(String[] args) throws IOException {
        String fromTime = null, toTime = null, path = null;
        for(int i = 0; i<args.length; i++) {
            //System.out.println(args[i]);
            if(args[i].equals("-f")){
                fromTime = args[i+1];
            }
            else if(args[i].equals("-t")){
                toTime = args[i+1];
            }
            else if(args[i].equals("-i")){
                path = args[i+1];
            }
        }

        //2020-06-21T10:11:17.734744Z
        //2020-06-21T10:50:57.203789Z

        //java -jar LogExtractorJava.jar -f 2020-06-21T10:11:17.734744Z -t 2020-06-21T10:50:57.203789Z -i C:\Users\ABHI\IdeaProjects\LogExtractorJava\logs\

        //java LogExtractor -f 2020-06-21T10:11:17.734744Z -t 2020-06-21T10:50:57.203789Z -i C:\Users\ABHI\IdeaProjects\LogExtractorJava\logs\


        //long startTime = System.nanoTime();
        //long ctr = 0;
//        String fromTime = "2020-06-21T10:55:32.006487Z";
//        String toTime = "2020-06-26T05:21:33.677242Z";
        Date fromDateTime = getDateTime(fromTime);
        Date toDateTime = getDateTime(toTime);
        //System.out.println(fromDateTime);
        TreeSet<String> fileList = findFile(fromDateTime, toDateTime);
        //System.out.println(fileList);
        for(String file: fileList) {
            try { //LogFile-000001.log
                //System.out.println(path + file);
                RandomAccessFile aFile = new RandomAccessFile(path+file, "r");
                FileChannel inChannel = aFile.getChannel();
                MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
                StringBuilder str = new StringBuilder();
                boolean flag = true;
                int startIndex = 0;
                if(file.equals(fileList.first()))
                    startIndex = binary_search(0,buffer.limit(),buffer,fromDateTime);
                else
                    startIndex = 0;
                //System.out.println("Start Index : " + startIndex);
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

        }

        //long endTime = System.nanoTime();
        //long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
        //System.out.println("Count : "+ ctr );
        //System.out.println("Total elapsed time: " + elapsedTimeInMillis + " ms");
    }
}

//LogFile-000001.log 2020-06-21T09:10:26.716773Z 2020-06-21T10:11:17.736739Z
//        LogFile-000002.log 2020-06-21T10:50:57.188179Z 2020-06-21T10:55:32.007485Z
//        LogFile-000003.log 2020-06-26T05:21:33.661573Z 2020-06-26T05:21:57.913868Z
