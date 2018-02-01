package eu.innovation.engineering.persistence;


public abstract class DbApplication {
  public static SQLiteVectors dbVectors = new SQLiteVectors("app/databases/databaseVectors.db");
  public static SQLiteWikipediaGraph dbGraph = new SQLiteWikipediaGraph("app/databases/databaseWikipediaGraph.db");
}
