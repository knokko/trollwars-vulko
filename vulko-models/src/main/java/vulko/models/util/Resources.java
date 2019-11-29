package vulko.models.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Resources {

    public static String[] readResourceAsStrings(String path){
        InputStream input = Resources.class.getClassLoader().getResourceAsStream(path);
        InputStreamReader inputReader = new InputStreamReader(input);
        BufferedReader reader = new BufferedReader(inputReader);

        try {
            List<String> lineList = new ArrayList<>(input.available() / 10);

            String line = reader.readLine();
            while (line != null){
                lineList.add(line);
                line = reader.readLine();
            }

            input.close();

            String[] result = new String[lineList.size()];
            lineList.toArray(result);
            return result;
        } catch (IOException ex){
            throw new Error(ex);
        }
    }

    public static String readResourceAsString(String path, String lineSeparator){
        String[] lines = readResourceAsStrings(path);
        int totalLength = 0;
        for (String line : lines){
            totalLength += line.length() + lineSeparator.length();
        }

        char[] allChars = new char[totalLength];
        int index = 0;
        for (String line : lines){
            line.getChars(0, line.length(), allChars, index);
            index += line.length();
            lineSeparator.getChars(0, lineSeparator.length(), allChars, index);
            index += lineSeparator.length();
        }

        return new String(allChars);
    }
}
