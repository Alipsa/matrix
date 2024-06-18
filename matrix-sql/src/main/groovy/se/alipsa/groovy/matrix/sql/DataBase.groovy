package se.alipsa.groovy.matrix.sql

enum DataBase {

  H2('jdbc:h2:')

  String urlStart

  DataBase(String urlStart) {
    this.urlStart = urlStart
  }

  String getUrlStart() {
    return urlStart
  }

  static DataBase fromUrl(String url) {
    for (DataBase db : values()) {
      if (url.startsWith(db.urlStart)) {
        return db
      }
    }
    return null
  }

}