package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic

/**
 * ColorBrewer palette definitions and helpers.
 */
@CompileStatic
class BrewerPalettes {

  private static final Map<String, List<String>> PALETTES = [
      // Qualitative
      'accent': ['#7FC97F', '#BEAED4', '#FDC086', '#FFFF99', '#386CB0', '#F0027F', '#BF5B17', '#666666'],
      'dark2': ['#1B9E77', '#D95F02', '#7570B3', '#E7298A', '#66A61E', '#E6AB02', '#A6761D', '#666666'],
      'paired': ['#A6CEE3', '#1F78B4', '#B2DF8A', '#33A02C', '#FB9A99', '#E31A1C',
          '#FDBF6F', '#FF7F00', '#CAB2D6', '#6A3D9A', '#FFFF99', '#B15928'],
      'pastel1': ['#FBB4AE', '#B3CDE3', '#CCEBC5', '#DECBE4', '#FED9A6', '#FFFFCC', '#E5D8BD', '#FDDAEC', '#F2F2F2'],
      'pastel2': ['#B3E2CD', '#FDCDAC', '#CBD5E8', '#F4CAE4', '#E6F5C9', '#FFF2AE', '#F1E2CC', '#CCCCCC'],
      'set1': ['#E41A1C', '#377EB8', '#4DAF4A', '#984EA3', '#FF7F00', '#FFFF33', '#A65628', '#F781BF', '#999999'],
      'set2': ['#66C2A5', '#FC8D62', '#8DA0CB', '#E78AC3', '#A6D854', '#FFD92F', '#E5C494', '#B3B3B3'],
      'set3': ['#8DD3C7', '#FFFFB3', '#BEBADA', '#FB8072', '#80B1D3', '#FDB462',
          '#B3DE69', '#FCCDE5', '#D9D9D9', '#BC80BD', '#CCEBC5', '#FFED6F'],
      // Sequential
      'blues': ['#F7FBFF', '#DEEBF7', '#C6DBEF', '#9ECAE1', '#6BAED6', '#4292C6', '#2171B5', '#08519C', '#08306B'],
      'greens': ['#F7FCF5', '#E5F5E0', '#C7E9C0', '#A1D99B', '#74C476', '#41AB5D', '#238B45', '#006D2C', '#00441B'],
      'greys': ['#FFFFFF', '#F0F0F0', '#D9D9D9', '#BDBDBD', '#969696', '#737373', '#525252', '#252525', '#000000'],
      'oranges': ['#FFF5EB', '#FEE6CE', '#FDD0A2', '#FDAE6B', '#FD8D3C', '#F16913', '#D94801', '#A63603', '#7F2704'],
      'purples': ['#FCFBFD', '#EFEDF5', '#DADAEB', '#BCBDDC', '#9E9AC8', '#807DBA', '#6A51A3', '#54278F', '#3F007D'],
      'reds': ['#FFF5F0', '#FEE0D2', '#FCBBA1', '#FC9272', '#FB6A4A', '#EF3B2C', '#CB181D', '#A50F15', '#67000D'],
      'bugn': ['#F7FCFD', '#E5F5F9', '#CCECE6', '#99D8C9', '#66C2A4', '#41AE76', '#238B45', '#006D2C', '#00441B'],
      'bupu': ['#F7FCFD', '#E0ECF4', '#BFD3E6', '#9EBCDA', '#8C96C6', '#8C6BB1', '#88419D', '#810F7C', '#4D004B'],
      'gnbu': ['#F7FCF0', '#E0F3DB', '#CCEBC5', '#A8DDB5', '#7BCCC4', '#4EB3D3', '#2B8CBE', '#0868AC', '#084081'],
      'orrd': ['#FFF7EC', '#FEE8C8', '#FDD49E', '#FDBB84', '#FC8D59', '#EF6548', '#D7301F', '#B30000', '#7F0000'],
      'pubu': ['#FFF7FB', '#ECE7F2', '#D0D1E6', '#A6BDDB', '#74A9CF', '#3690C0', '#0570B0', '#045A8D', '#023858'],
      'pubugn': ['#FFF7FB', '#ECE2F0', '#D0D1E6', '#A6BDDB', '#67A9CF', '#3690C0', '#02818A', '#016C59', '#014636'],
      'purd': ['#F7F4F9', '#E7E1EF', '#D4B9DA', '#C994C7', '#DF65B0', '#E7298A', '#CE1256', '#980043', '#67001F'],
      'rdpu': ['#FFF7F3', '#FDE0DD', '#FCC5C0', '#FA9FB5', '#F768A1', '#DD3497', '#AE017E', '#7A0177', '#49006A'],
      'ylgn': ['#FFFFE5', '#F7FCB9', '#D9F0A3', '#ADDD8E', '#78C679', '#41AB5D', '#238443', '#006837', '#004529'],
      'ylgnbu': ['#FFFFD9', '#EDF8B1', '#C7E9B4', '#7FCDBB', '#41B6C4', '#1D91C0', '#225EA8', '#253494', '#081D58'],
      'ylorbr': ['#FFFFE5', '#FFF7BC', '#FEE391', '#FEC44F', '#FE9929', '#EC7014', '#CC4C02', '#993404', '#662506'],
      'ylorrd': ['#FFFFCC', '#FFEDA0', '#FED976', '#FEB24C', '#FD8D3C', '#FC4E2A', '#E31A1C', '#BD0026', '#800026'],
      // Diverging
      'brbg': ['#543005', '#8C510A', '#BF812D', '#DFC27D', '#F6E8C3', '#F5F5F5', '#C7EAE5', '#80CDC1', '#35978F', '#01665E', '#003C30'],
      'piyg': ['#8E0152', '#C51B7D', '#DE77AE', '#F1B6DA', '#FDE0EF', '#F7F7F7', '#E6F5D0', '#B8E186', '#7FBC41', '#4D9221', '#276419'],
      'prgn': ['#40004B', '#762A83', '#9970AB', '#C2A5CF', '#E7D4E8', '#F7F7F7', '#D9F0D3', '#A6DBA0', '#5AAE61', '#1B7837', '#00441B'],
      'puor': ['#7F3B08', '#B35806', '#E08214', '#F1A340', '#FDDDB5', '#F7F7F7', '#D8DAEB', '#B2ABD2', '#8073AC', '#542788', '#2D004B'],
      'rdbu': ['#67001F', '#B2182B', '#D6604D', '#F4A582', '#FDDBC7', '#F7F7F7', '#D1E5F0', '#92C5DE', '#4393C3', '#2166AC', '#053061'],
      'rdgy': ['#67001F', '#B2182B', '#D6604D', '#F4A582', '#FDDBC7', '#FFFFFF', '#E0E0E0', '#BABABA', '#878787', '#4D4D4D', '#1A1A1A'],
      'rdylbu': ['#A50026', '#D73027', '#F46D43', '#FDAE61', '#FEE090', '#FFFFBF', '#E0F3F8', '#ABD9E9', '#74ADD1', '#4575B4', '#313695'],
      'rdylgn': ['#A50026', '#D73027', '#F46D43', '#FDAE61', '#FEE08B', '#FFFFBF', '#D9EF8B', '#A6D96A', '#66BD63', '#1A9850', '#006837'],
      'spectral': ['#9E0142', '#D53E4F', '#F46D43', '#FDAE61', '#FEE08B', '#FFFFBF', '#E6F598', '#ABDDA4', '#66C2A5', '#3288BD', '#5E4FA2']
  ] as Map<String, List<String>>

  /**
   * Resolve a palette by name.
   *
   * @param name palette name (case-insensitive)
   * @return list of hex colors or null if not found
   */
  static List<String> getPalette(String name) {
    if (name == null) {
      return null
    }
    PALETTES[name.toLowerCase()]
  }

  /**
   * Get list of palette names for a given type.
   *
   * @param type 'seq', 'div', or 'qual'
   * @return list of palette names for that type
   */
  static List<String> getPaletteNamesForType(String type) {
    final Map<String, List<String>> typeMap = [
        'seq': ['blues', 'greens', 'greys', 'oranges', 'purples', 'reds',
                'bugn', 'bupu', 'gnbu', 'orrd', 'pubu', 'pubugn', 'purd', 'rdpu',
                'ylgn', 'ylgnbu', 'ylorbr', 'ylorrd'],
        'sequential': ['blues', 'greens', 'greys', 'oranges', 'purples', 'reds',
                       'bugn', 'bupu', 'gnbu', 'orrd', 'pubu', 'pubugn', 'purd', 'rdpu',
                       'ylgn', 'ylgnbu', 'ylorbr', 'ylorrd'],
        'div': ['brbg', 'piyg', 'prgn', 'puor', 'rdbu', 'rdgy', 'rdylbu', 'rdylgn', 'spectral'],
        'diverging': ['brbg', 'piyg', 'prgn', 'puor', 'rdbu', 'rdgy', 'rdylbu', 'rdylgn', 'spectral'],
        'qual': ['accent', 'dark2', 'paired', 'pastel1', 'pastel2', 'set1', 'set2', 'set3'],
        'qualitative': ['accent', 'dark2', 'paired', 'pastel1', 'pastel2', 'set1', 'set2', 'set3']
    ]
    typeMap[type?.toLowerCase()] ?: []
  }

  /**
   * Get a palette by numeric index for a given type.
   *
   * @param type 'seq', 'div', or 'qual'
   * @param index 1-based palette index
   * @return palette name or null if index is out of range
   */
  static String getPaletteNameByIndex(String type, int index) {
    List<String> names = getPaletteNamesForType(type)
    if (index < 1 || index > names.size()) {
      return null
    }
    names[index - 1]
  }

  /**
   * Select a palette with a target size and optional direction.
   *
   * @param name palette name
   * @param n number of colors needed
   * @param direction 1 for normal, -1 for reversed
   * @return list of colors
   */
  static List<String> selectPalette(String name, int n, int direction = 1) {
    List<String> colors = getPalette(name)
    if (colors == null || colors.isEmpty()) {
      return []
    }
    List<String> selected = selectColors(colors, n)
    if (direction < 0) {
      return selected.reverse()
    }
    selected
  }

  private static List<String> selectColors(List<String> colors, int n) {
    if (n <= 0) return []
    if (colors.size() == n) return new ArrayList<>(colors)
    if (n == 1) {
      return [colors[(colors.size() / 2.0).floor() as int]] as List<String>
    }
    if (n >= colors.size()) {
      List<String> expanded = []
      for (int i = 0; i < n; i++) {
        expanded << colors[i % colors.size()]
      }
      return expanded
    }
    List<String> selected = []
    for (int i = 0; i < n; i++) {
      BigDecimal idx = i * (colors.size() - 1) / (n - 1 as BigDecimal)
      int rounded = idx.round() as int
      selected << colors[rounded]
    }
    selected
  }
}
