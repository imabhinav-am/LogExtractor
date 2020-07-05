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

public class LogExtractor {

    /**
     * Method to find file in which log interval lies, there can be two cases
     * Interval in one file only
     * Interval starts from one file but ends to other file
     * @param fromDateTime Log start time
     * @param toDateTime Log end time
     * @return Set of File Names
     */
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
        Integer j=-1;
        for (Integer i=0; i<lines.length; i++) {
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

    /**
     * Method to convert String to IS0 format Date Time
     * @param str Date in string format
     * @return Date object of string
     */
    public static Date getDateTime(String str){
        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(str);
        Instant it = Instant.from(ta);
        return Date.from(it);
    }

    /**
     * Method to check whether the time at middle as per binary search is
     * less, greater or equal to the start time of log interval.
     * @param index Starting index to read buffer
     * @param buffer File buffer read
     * @param fromTime Start time of Log interval
     * @return 0,1, or -1 depending on whether time is equal, greater or less than fromTime respectively
     */
    public static Integer check_time(Integer index, MappedByteBuffer buffer, Date fromTime){
        StringBuilder str = new StringBuilder();
        for(Integer i=index; i<buffer.limit(); i++){
            byte read = buffer.get(i);
            char res = (char)read;
            str.append(res);
            if(res == 'Z') {
                if(str.length()==27)
                    break;
            }
            if(res=='\n') {
                str.setLength(0);
            }
        }

        Date foundDateTime = getDateTime(str.toString());
        return foundDateTime.compareTo(fromTime);
    }

    /**
     * Method to binary search the file for finding the start time of log interval required in the file
     * @param lo Lower index for binary search
     * @param hi Higher index for binary search
     * @param buffer File buffer read
     * @param fromTime Start time of Log interval
     * @return index from which output should be generated
     */
    public static Integer binary_search(Integer lo,Integer hi,MappedByteBuffer buffer,Date fromTime) {
        Integer mid=0;
        while (lo < hi) {
            mid = lo + (hi - lo) / 2;
            if (check_time(mid, buffer, fromTime) >= 0)
                hi = mid;
            else
                lo = mid + 1;
        }
        StringBuilder str = new StringBuilder();
        for(Integer i=lo; i<buffer.limit(); i++){
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

    public static void main(String[] args) {
        String fromTime = null, toTime = null, path = null;
//        for(Integer i = 0; i<args.length; i++) {
//            switch (args[i]) {
//                case "-f":
//                    fromTime = args[i + 1];
//                    break;
//                case "-t":
//                    toTime = args[i + 1];
//                    break;
//                case "-i":
//                    path = args[i + 1];
//                    break;
//            }
//        }
//        fromTime = "2020-06-26T17:50:26.6037Z";
//        toTime = "2020-06-26T17:50:26.8037Z";
        fromTime = "2020-06-26T17:27:43.0122Z";
        toTime = "2020-06-26T17:27:43.0151Z";
        path = "C:\\Users\\ABHI\\IdeaProjects\\LogExtractor\\logs\\";

        Date fromDateTime = getDateTime(fromTime);
        Date toDateTime = getDateTime(toTime);
        TreeSet<String> fileList = findFile(fromDateTime, toDateTime);
        for(String file: fileList) {
            try {
                RandomAccessFile aFile = new RandomAccessFile(path+file, "r");
                FileChannel inChannel = aFile.getChannel();
                System.out.println(inChannel.size());
                MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, 6638794712L);
                StringBuilder str = new StringBuilder();
                boolean flag = true;
                Integer startIndex = 0;
                if(file.equals(fileList.first()))
                    startIndex = binary_search(0,buffer.limit(),buffer,fromDateTime);
                else
                    startIndex = 0;
                for (Integer i = startIndex; i < buffer.limit(); i++) {
                    byte read = buffer.get(i);
                    char res = (char)read;
                    System.out.print(res);
                    if(flag)
                        str.append(res);
                    if(res == 'Z') {
                        if(toTime.equals(str.toString())) {
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
    }
}
