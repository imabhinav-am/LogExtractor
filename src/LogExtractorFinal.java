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

public class LogExtractorFinal {

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
        int j=-1;
        for (int i = 0; i<lines.length; i++) {
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
     * Method to binary search the file for finding the start time of log interval required in the file
     * @param lo Lower index for binary search
     * @param hi Higher index for binary search
     * @param buffer File buffer read
     * @param fromTime Start time of Log interval
     * @return index from which output should be generated
     */
    public static Integer binary_search(int lo,int hi,MappedByteBuffer buffer,Date fromTime) {
        int mid;
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
                if(str.length()==25)
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
     * Method to convert String to IS0 format Date Time
     * @param str Date in string format
     * @return Date object of string
     */
    public static Date getDateTime(String str){
        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(str);
        Instant it = Instant.from(ta);
        return Date.from(it);
    }

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        String fromTime = null, toTime = null, path = null;
        for(int i = 0; i<args.length; i++) {
            switch (args[i]) {
                case "-f":
                    fromTime = args[i + 1];
                    break;
                case "-t":
                    toTime = args[i + 1];
                    break;
                case "-i":
                    path = args[i + 1];
                    break;
            }
        }
        Date fromDateTime = getDateTime(fromTime);
        Date toDateTime = getDateTime(toTime);
        if(fromDateTime.compareTo(toDateTime)>=0){
            System.out.println("ERROR: ToTime must be greater than FromTime.");
            System.exit(0);
        }
        try {
            TreeSet<String> fileList = findFile(fromDateTime, toDateTime);
            for (String file : fileList) {
                RandomAccessFile aFile = new RandomAccessFile(path + file, "r");
                FileChannel inChannel = aFile.getChannel();
                long size = inChannel.size();
                long i = 2000000000, j = 0;
                MappedByteBuffer buffer;
                if (file.equals(fileList.first())) {
                    while (i < size) {
                        buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, j, 2000000000);
                        int chk = check_time(0, buffer, getDateTime(fromTime));
                        if (chk >= 0) {
                            break;
                        }
                        j = i;
                        if (size - i >= 2000000000) {
                            i += 2000000000;
                        } else {
                            i = size;
                            break;
                        }
                    }
                    long startIndex;
                    if (i == size) {
                        buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, j, i - j);
                        startIndex = binary_search(0, buffer.limit(), buffer, getDateTime(fromTime));
                        buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, startIndex + j, i - (startIndex + j));
                    } else if (j != 0) {
                        long pos = j - 2000000000;
                        buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, pos, 2000000000);
                        startIndex = binary_search(0, 2000000000, buffer, getDateTime(fromTime));
                        buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, startIndex + pos, 200000000);
                    } else {
                        buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, 2000000000);
                    }
                } else {
                    buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, 10000000);
                }
                StringBuilder str = new StringBuilder();
                Date foundTime;
                for (int x = 0; x < buffer.limit(); x++) {
                    byte read = buffer.get(x);
                    char res = (char) read;
                    str.append(res);
                    if (res == 'Z') {
                        foundTime = getDateTime(str.toString());
                        if (foundTime.compareTo(toDateTime) > 0) {
                            break;
                        }
                    }
                    if (res == '\n') {
                        System.out.print(str);
                        str.setLength(0);
                    }
                }
            }
        }
        catch (IOException e){
            System.out.println(e.getMessage());
            System.exit(0);
        }
        long endTime = System.nanoTime();
        long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
        //System.out.println("Total elapsed time: " + elapsedTimeInMillis + " ms"  );
    }
}
