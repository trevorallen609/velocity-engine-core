import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.io.*;

public class FileReader {
    public static void main(String[] args) {
        InputParser inputParser = new InputParser(args);

        String directoryPath = inputParser.getDirectoryPath();
        String fileType = inputParser.getTypeValue();
        String startTime = inputParser.getStartTimeValue();
        String endTime = inputParser.getEndTimeValue();
        String outputFileType = inputParser.getOutputFileType();
        String outputFileName = inputParser.getOutputFileName();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        LocalDateTime startTimeDT = LocalDateTime.parse(startTime, formatter);
        LocalDateTime endTimeDT = LocalDateTime.parse(endTime, formatter);


        HashMap<String, HashMap<String, String>> data = new HashMap<>();

        File dir = new File(directoryPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(fileType));

        for (File file : files) {
            try {
                Scanner fileInput = new Scanner(file);

                if (fileType.equals("json")) {
                    HashMap<String, String> map = new HashMap<>();
                    boolean skipObject = false;

                    while (fileInput.hasNextLine()) {
                        String jsonLine = fileInput.nextLine();
                        String[] jsonValues = jsonLine.split(",");

                        for(String jsonValue : jsonValues) {
                            String[] keyValue = jsonValue.split(":");

                            if (keyValue.length == 2 || keyValue.length == 4) {
                                String key = keyValue[0].trim();
                                String value = keyValue[1].trim();
                                key = key.substring(1, key.length()-1);

                                if (key.equals("value")) {
                                  key = "total_value";
                                }

                                if (key.equals("total_value") || key.equals("level_name")) {
                                  map.put(key, String.valueOf(value));
                                } else if (key.equals("timestamp")) {
                                  String time = jsonValue.trim().split(" ")[1];
                                  time = time.substring(1, time.length()-1);
                                  LocalDateTime timestamp = LocalDateTime.parse(time, formatter);

                                  if (timestamp.isBefore(startTimeDT) || timestamp.isAfter(endTimeDT)) {
                                    skipObject = true;
                                  }
                                }
                            } else if(keyValue[0].trim().equals("}")) {
                              String levelName = map.get("level_name");

                              if (skipObject) {
                                skipObject = false;
                              }
                              else if (data.containsKey(levelName)) {
                                  HashMap<String, String> existingMap = data.get(levelName);
                                  Integer value =  parseInt(existingMap.get("total_value")) + parseInt(map.get("total_value"));
                                  existingMap.put("total_value", String.valueOf(value));
                              } else {
                                  data.put(levelName, map);
                              }
                              map = new HashMap<>();
                            }
                        }

                    }
                } else if (fileType.equals("csv")) {
                    if (fileInput.hasNextLine())
                      fileInput.nextLine();

                    while (fileInput.hasNextLine()) {
                        String csvLine = fileInput.nextLine();
                        String[] csvValues = csvLine.split(",");
                        LocalDateTime timestamp = LocalDateTime.parse(csvValues[0], formatter);

                        if ((timestamp.isAfter(startTimeDT) || timestamp.equals(startTimeDT)) && (timestamp.isBefore(endTimeDT) || timestamp.equals(endTimeDT))) {
                          HashMap<String, String> map = new HashMap<>();

                          map.put("level_name", csvValues[1]);
                          map.put("value", csvValues[2]);

                          String levelName = map.get("level_name");

                          if (data.containsKey(levelName)) {
                              HashMap<String, String> existingMap = data.get(levelName);
                              Integer value = parseInt(existingMap.get("value")) + parseInt(map.get("value"));
                              existingMap.put("value", String.valueOf(value));
                          } else {
                              data.put(levelName, map);
                          }
                        }
                    }
                }
                fileInput.close();
            } catch (FileNotFoundException e) {
                System.out.println("File not found: " + file.getName());
            }
        }

        display(data);


        if (outputFileType.equals("json")) {
          writeInJsonFile(data, outputFileName);
        } else {
          writeInYamlFile(data, outputFileName);
        }
    }

    public static void display(HashMap<String, HashMap<String, String>> data) {
      for (String key : data.keySet()) {
        HashMap<String, String> innerMap = data.get(key);

        for (String innerKey : innerMap.keySet())
            System.out.println("  " + innerKey + ": " + innerMap.get(innerKey));
      }
    }

    public static int parseInt(String valueString) {
      if (valueString == null || valueString.isEmpty()) return 0;

      try {
          return Integer.parseInt(valueString);
      } catch (NumberFormatException e) {
          return 0;
      }
    }

    public static void writeInYamlFile(HashMap<String, HashMap<String, String>> data, String outputFileName) {
        try {
            FileWriter fileWriter = new FileWriter(outputFileName);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (String key1 : data.keySet()) {
                HashMap<String, String> subData = data.get(key1);
                bufferedWriter.write("- ");
                boolean firstKey = true;

                for (String valueKey : subData.keySet()) {
                  String value = subData.get(valueKey);

                  if (!firstKey) bufferedWriter.write("  ");

                  firstKey = !firstKey;
                  bufferedWriter.write(valueKey + ": " +  "\"" + value + "\"");
                  bufferedWriter.newLine();
                }
            }

            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeInJsonFile(HashMap<String, HashMap<String, String>> data, String outputFileName) {
      try {
          FileWriter fileWriter = new FileWriter(outputFileName);
          BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

          // Write the header row
          bufferedWriter.write("[");
          bufferedWriter.newLine();

          for (String key1 : data.keySet()) {
              HashMap<String, String> subData = data.get(key1);
              bufferedWriter.write("  {");
              bufferedWriter.newLine();
              boolean isLastElement = false;
              for (String valueKey : subData.keySet()) {
                  String value = subData.get(valueKey);

                  bufferedWriter.write("    " + "\"" + valueKey + "\"" + ":" + "  " + value);

                  if (!isLastElement) bufferedWriter.write(",");

                  bufferedWriter.newLine();
                  isLastElement = !isLastElement;
              }
              bufferedWriter.write("  },");
              bufferedWriter.newLine();
          }

          bufferedWriter.write("]");

          bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

<!-- Updated: 2024-05-28T11:12:00.582740 -->

<!-- Updated: 2024-06-26T13:42:00.582740 -->

<!-- Updated: 2024-07-05T10:06:00.582740 -->

<!-- Updated: 2024-07-08T16:08:00.582740 -->

<!-- Updated: 2024-07-16T09:47:00.582740 -->
