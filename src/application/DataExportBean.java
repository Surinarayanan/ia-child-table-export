package application;

/**
 * Created by Suriyanarayanan K
 * on 10/06/20 4:23 PM.
 */
public class DataExportBean {

    public static String dataFilePath = "";
    public static String jobOutputPath = "";
    public static boolean isRefactor = false;
    public static Thread currentThread = null;
    public static boolean isMultipleCsvFile = false;
    public static long headerLastPosition = 0;
}
