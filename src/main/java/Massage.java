import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Massage
{
    
    public static void main(String[] args) throws IOException
    {
        String mappings = System.getProperty("mapping","io/netty/=NETTY[j],org/apache/cassandra/utils/flow=FLOW[j],io/reactivex=RX[j]");
        boolean keepTid = Boolean.getBoolean("keepTid");
        Map<String,String> prefix2fold = new HashMap<>();
        String[] elements = mappings.split(",");
        for (String element : elements)
        {
            String[] kv = element.split("=");
            prefix2fold.put(kv[0],kv[1]);
        }
        Set<String> javaThreads = new HashSet<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null)
        {
            int countSeparatorIndex = line.lastIndexOf(" ");
            final String stack = line.substring(0, countSeparatorIndex);
            final String count = line.substring(countSeparatorIndex + 1);
            int postPrefix = stack.indexOf(";") + 1;
            final String prefix = stack.substring(0, postPrefix);
            String tmp = keepTid && prefix.startsWith("java-") ? stack : stack.substring(postPrefix);

            if (stack.contains(";GCTaskThread") ||
                stack.contains(";CompilerThread::") ||
                stack.contains(";GangWorker::") ||
                stack.contains(";WatcherThread::") ||
                stack.contains(";ConcurrentG1RefineThread::") ||
                stack.contains(";VMThread") ||
                stack.contains(";G1ParScanThreadState::") ||
                stack.contains(";G1RemSet::") ||
                stack.contains("CompileBroker"))
            {
                System.out.print("JVMThread;");
                printRemaining(count, tmp, prefix2fold);
            }
            else if (javaThreads.contains(prefix) ||
                stack.contains("[j]") ||
                stack.contains("[i]") ||
                stack.contains("Interpreter") ||
                stack.contains("..."))
            {
                System.out.print("JavaThread[j];");
                int ix;
                
                // trim stacks up to first Java frame
                if ((ix = stack.indexOf("Interpreter")) != -1 || (ix=stack.indexOf("java/lang/Thread")) != -1)
                {
                    if (prefix.startsWith("java-"))
                    {
                        javaThreads.add(prefix);
                        tmp = keepTid ? prefix + stack.substring(ix) : stack.substring(ix);
                    }
                    else
                    {
                        tmp = stack.substring(ix);
                    }
                }
                printRemaining(count, tmp, prefix2fold);
            }
            else
            {
                System.out.println(tmp + " " + count);
            }
        }
    }

    private static void printRemaining(
        String count,
        String stack,
        Map<String, String> prefix2folds)
    {
        String[] frames = stack.split(";");
        System.out.print(frames[0]);
        for (int i = 1; i < frames.length; i++)
        {
            for (Map.Entry<String,String> prefix2fold : prefix2folds.entrySet())
            {
                if (frames[i].startsWith(prefix2fold.getKey()))
                {
                    frames[i] = prefix2fold.getValue();
                    break;
                }
            }
            
            // skip same frame by ref equality -> the folded frames
            if (frames[i] != frames[i - 1])
            {
                System.out.print(";");
                System.out.print(frames[i]);
            }
        }
        System.out.print(" ");
        System.out.print(count);
        System.out.println();
    }
}
