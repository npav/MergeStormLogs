import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Scanner;

/**
 * Created by npav on 13/12/2016.
 */
public class CombineLogs {
  private static final String BASE_DIR = "logs";
  private static PrintWriter writer;
  private static PriorityQueue<String> logs;
  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  //2016-12-12T17:00:29.533+0200

  public static void main(String[] args) {
    try {
      writer = new PrintWriter(new FileOutputStream(new File("merged.log")), true);
      loadAllLogsInMemory();
      writeAllLogsInOneFileSorted();
      writer.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  private static void writeAllLogsInOneFileSorted() {
    while (!logs.isEmpty()) {
      writer.println(logs.poll().split(",", 2)[1]);
    }
  }

  private static void loadAllLogsInMemory() throws FileNotFoundException {
    logs = new PriorityQueue<>(new TimestampComparator());
    File baseDir = new File(BASE_DIR);
    for (File node : baseDir.listFiles()) {
      loadThisNodeLogs(node);
    }
  }

  private static void loadThisNodeLogs(File node) throws FileNotFoundException {
    for (File worker : node.listFiles()) {
      loadThisWorkerLogs(node, worker);
    }
  }

  private static void loadThisWorkerLogs(File node, File worker) throws FileNotFoundException {
    String nodePrefix = node.getName().split("\\.", 2)[0];
    String workerPrefix = worker.getName().split("\\.", 2)[0].replace("orker-", "");
    Scanner scanner = new Scanner(new FileInputStream(worker));
    String currentLine = scanner.nextLine();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (!line.startsWith("201")) { // If it does not start with a timestamp, append to previous
        currentLine += "\n " + line;
      } else {
        addThisLineToQueue(nodePrefix, workerPrefix, currentLine);
        currentLine = line;
      }
    }
    //Also do it for the last line
    addThisLineToQueue(nodePrefix, workerPrefix, currentLine);
  }

  private static void addThisLineToQueue(String nodePrefix, String workerPrefix, String currentLine) {
    try {
      long timestamp = sdf.parse(currentLine.split("\\+", 2)[0]).getTime();
      String newLineToWrite = timestamp + "," + nodePrefix + "," + workerPrefix + "," + currentLine;
      logs.add(newLineToWrite);
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  private static class TimestampComparator implements Comparator<String> {

    @Override public int compare(String l1, String l2) {
      long t1 = Long.parseLong(l1.split(",", 2)[0]);
      long t2 = Long.parseLong(l2.split(",", 2)[0]);
      return (int) (t1 - t2);
    }
  }
}
