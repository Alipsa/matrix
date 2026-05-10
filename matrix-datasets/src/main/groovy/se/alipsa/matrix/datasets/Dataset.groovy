package se.alipsa.matrix.datasets

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.datasets.util.FileUtil

/**
 * Provides access to common datasets in Matrix format.
 * Includes statistical datasets (mtcars, iris, diamonds, etc.) and geographical map data.
 */
@CompileStatic
class Dataset {

  private static final Logger log = Logger.getLogger(Dataset)

  /** Map dataset file paths indexed by exact name */
  private static final Map<String, String> MAP_DATA_FILES = [
      county: '/data/maps/map_county.csv',
      france: '/data/maps/map_france.csv',
      italy : '/data/maps/map_italy.csv',
      nz    : '/data/maps/map_nz.csv',
      state : '/data/maps/map_state.csv',
      usa   : '/data/maps/map_usa.csv',
      world : '/data/maps/map_world.csv',
      world2: '/data/maps/map_world2.csv'
  ]

  /** Prefix mappings for non-exact matching (prefix -> exact name) */
  private static final Map<String, String> MAP_DATA_PREFIXES = [
      co: 'county',
      fr: 'france',
      it: 'italy',
      nz: 'nz',
      st: 'state',
      us: 'usa'
  ]

  private static final String COMMA = ','
  private static final String QUOTE = '"'
  private static final String ORDER = 'order'
  private static final String GROUP = 'group'
  private static final String REGION = 'region'

  static Matrix airquality() {
    Matrix.builder()
        .matrixName('airquality')
        .data(url('/data/airquality.csv'), COMMA, '', true)
        .build()
        .convert(
            'Ozone': BigDecimal,
            'Solar.R': BigDecimal,
            'Wind': BigDecimal,
            'Temp': BigDecimal,
            'Month': Short,
            'Day': Short
        )
  }

  static Matrix cars() {
    Matrix.builder()
        .matrixName('cars')
        .data(url('/data/cars.csv'), COMMA, QUOTE)
        .build()
        .convert(
            speed: BigDecimal,
            dist: BigDecimal
        )
  }

  /**
   * Measurements in centimeters of the variables sepal length, sepal width,
   * petal length and petal width, respectively, for 50 flowers from each of 3 species of iris.
   */
  static Matrix iris() {
    Matrix.builder()
        .matrixName('Iris')
        .data(url('/data/iris.csv'))
        .build()
        .convert(
            'Sepal Length': BigDecimal,
            'Sepal Width': BigDecimal,
            'Petal Length': BigDecimal,
            'Petal Width': BigDecimal,
            Species: String
        )
  }

  /**
   * Fuel consumption and 10 aspects of automobile design and performance for 32 automobiles
   */
  static Matrix mtcars() {
    Matrix.builder()
        .matrixName('mtcars')
        .data(url('/data/mtcars.csv'))
        .build()
        .convert(
            model: String,
            mpg: BigDecimal,
            cyl: Integer,
            disp: BigDecimal,
            hp: BigDecimal,
            drat: BigDecimal,
            wt: BigDecimal,
            qsec: BigDecimal,
            vs: BigDecimal,
            am: Integer,
            gear: Integer,
            carb: Integer
        )
  }

  /**
   * Contains results obtained from an experiment to compare yields
   * (as measured by dried weight of plants)
   * obtained under a control and two different treatment conditions
   */
  static Matrix plantGrowth() {
    Matrix.builder()
        .matrixName('PlantGrowth')
        .data(url('/data/PlantGrowth.csv'), COMMA, QUOTE)
        .build()
        .convert(
            id: Integer,
            weight: BigDecimal
        )
  }

  /**
   * The result from an experiment studying the effect of
   * vitamin C on tooth growth in 60 Guinea pigs
   */
  static Matrix toothGrowth() {
    Matrix.builder()
        .matrixName('ToothGrowth')
        .data(url('/data/ToothGrowth.csv'), COMMA, QUOTE)
        .build()
        .convert(
            id: Integer,
            len: BigDecimal,
            dose: BigDecimal
        )
  }

  /**
   * Statistics of arrests per 100,000 residents for assault, murder, and rape
   * in each of the 50 US states in 1973
   */
  static Matrix usArrests() {
    Matrix.builder()
        .matrixName('USArrests')
        .data(url('/data/USArrests.csv'), COMMA, QUOTE)
        .build()
        .convert(
            'Murder': BigDecimal,
            'Assault': Integer,
            'UrbanPop': Integer,
            'Rape': BigDecimal
        )
  }

  /**
   * Includes information about the fuel economy of popular car models in 1999 and 2008,
   * collected by the US Environmental Protection Agency, http://fueleconomy.gov.
   */
  static Matrix mpg() {
    Matrix.builder()
        .matrixName('mpg')
        .data(url('/data/mpg.csv'), COMMA, QUOTE)
        .build()
        .convert(
            'manufacturer': String,
            'model': String,
            'displ': BigDecimal,
            'year': Integer,
            'cyl': Integer,
            'trans': String,
            'drv': String,
            'cty': Integer,
            'hwy': Integer,
            'fl': String,
            'class': String
        )
  }

  static Matrix diamonds() {
    Matrix.builder()
        .matrixName('diamonds')
        .data(url('/data/diamonds.csv'), COMMA, QUOTE)
        .build()
        .convert(
            'carat': BigDecimal,
            'cut': String,
            'color': String,
            'clarity': String,
            'depth': BigDecimal,
            'table': BigDecimal,
            'price': Integer,
            'x': BigDecimal,
            'y': BigDecimal,
            'z': BigDecimal
        )
  }

  static Matrix fromUrl(String url, String delimiter = ',', String stringQuote = '') {
    Matrix.builder()
        .data(new URL(url), delimiter, stringQuote)
        .build()
  }

  /**
   * Loads geographical map data for the specified dataset.
   *
   * @param datasetName the name of the map dataset (county, france, italy, nz, state, usa, world, world2)
   * @param region optional region name to filter the data
   * @param exact if true, requires exact name match; if false, allows prefix matching
   * @return a Matrix containing the map data
   * @throws IllegalArgumentException if the dataset name is null or not found
   */
  static Matrix mapData(String datasetName, String region = null, boolean exact = false) {
    if (datasetName == null) {
      throw new IllegalArgumentException('dataset name cannot be null')
    }
    String name = datasetName.trim().toLowerCase(Locale.ROOT)
    String filePath = resolveMapDataFile(name, exact)

    if (filePath == null) {
      log.debug("Map dataset not found: $datasetName (exact=$exact)")
      throw new IllegalArgumentException("no map data exists for '$datasetName'. Valid names: ${mapNames().join(', ')}")
    }

    log.debug("Loading map data from $filePath" + (region ? " for region: $region" : ''))
    mapDataSet(filePath, region)
  }

  /**
   * Resolves the file path for a map dataset name.
   *
   * @param name the lowercase dataset name
   * @param exact if true, requires exact name match; if false, allows prefix matching
   * @return the file path or null if not found
   */
  private static String resolveMapDataFile(String name, boolean exact) {
    // Try exact match first
    if (MAP_DATA_FILES.containsKey(name)) {
      return MAP_DATA_FILES[name]
    }

    // If exact matching is required, no match found
    if (exact) {
      return null
    }

    // Try prefix matching
    String matchedName = MAP_DATA_PREFIXES.find { prefix, exactName ->
      name.startsWith(prefix)
    }?.value

    return matchedName ? MAP_DATA_FILES[matchedName] : null
  }

  @CompileDynamic
  static Matrix mapDataSet(String filePath, String region = null) {
    Matrix ds = Matrix.builder()
        .data(url(filePath), COMMA, QUOTE)
        .build()
        .convert([
            'long': BigDecimal,
            'lat': BigDecimal,
            'group': Integer,
            'order': Integer,
            (REGION): String,
            'subregion': String
        ])
    if (region == null) {
      log.debug("Loaded ${ds.rowCount()} rows from $filePath")
      return ds
    }
    Matrix sub = ds.subset(REGION) { it == region }
    if (sub.rowCount() == 0) {
      log.warn("Region not found in dataset: $region")
      throw new IllegalArgumentException("Region not found: $region")
    }
    log.debug("Filtered to ${sub.rowCount()} rows for region: $region")
    def minOrder = Stat.min(sub[ORDER]) - 1
    def minGroup = Stat.min(sub[GROUP]) - 1
    sub.apply(ORDER) { it - minOrder }
        .apply(GROUP) { it - minGroup }
        .orderBy(ORDER)
  }

  /**
   * Loads the specified map dataset and returns a sorted list of distinct region values.
   * <p>
   * <b>Performance note:</b> This loads the full map CSV into memory to extract regions.
   *
   * @param datasetName the name of the map dataset
   * @return a sorted list of distinct region values
   * @throws IllegalArgumentException if the dataset name is null or not found
   */
  static List<String> mapRegions(String datasetName) {
    def data = mapData(datasetName)
    def regions = data[REGION] as List<String>
    regions.unique().sort()
  }

  private static URL url(String path) {
    FileUtil.getResourceUrl(path)
  }

  /**
   * Provides a description of the dataset
   *
   * @param table , the dataset to describe
   * @return a String describing the content of the dataset
   */
  static String describe(Matrix table) {
    describe(table.matrixName)
  }

  /**
   * Provides a description of the dataset
   *
   * @param tableName the name of the dataset to describe
   * @return a String describing the content of the dataset
   */
  private static final Map<String, Closure<String>> DESCRIBERS = [
      airquality : { descAirquality() },
      cars       : { descCars() },
      mtcars     : { descMtcars() },
      iris       : { descIris() },
      plantgrowth: { descPlantGrowth() },
      toothgrowth: { descToothGrowth() },
      usarrests  : { descUsArrests() },
      mpg        : { descMpg() },
      diamonds   : { descDiamonds() },
      mapdata    : { descMapData() },
      map_data   : { descMapData() }
  ]

  static String describe(String tableName) {
    DESCRIBERS.get(tableName.toLowerCase(Locale.ROOT))?.call() ?: "Unknown table: ${tableName}"
  }

  private static final Map<String, Closure<Matrix>> DATASET_LOADERS = [
      airquality : { airquality() },
      cars       : { cars() },
      mtcars     : { mtcars() },
      iris       : { iris() },
      plantgrowth: { plantGrowth() },
      toothgrowth: { toothGrowth() },
      usarrests  : { usArrests() },
      mpg        : { mpg() },
      diamonds   : { diamonds() }
  ]

  /**
   * Returns a sorted list of built-in dataset names.
   *
   * @return a sorted list of dataset names
   */
  static List<String> names() {
    DATASET_LOADERS.keySet().sort()
  }

  /**
   * Returns a sorted list of valid map dataset names.
   *
   * @return a sorted list of map dataset names
   */
  static List<String> mapNames() {
    MAP_DATA_FILES.keySet().sort()
  }

  /**
   * Loads a built-in dataset by name.
   *
   * @param name the name of the dataset to load
   * @return a Matrix containing the dataset
   * @throws IllegalArgumentException if the dataset name is unknown
   */
  static Matrix load(String name) {
    def loader = DATASET_LOADERS.get(name.toLowerCase(Locale.ROOT))
    if (loader == null) {
      throw new IllegalArgumentException("Unknown dataset: ${name}")
    }
    loader.call()
  }

  static String descAirquality() {
    '''
        Daily air quality measurements in New York, May to September 1973.

        Original source: Chambers, J. M., Cleveland, W. S., Kleiner, B. and Tukey, P. A. (1983)
        Graphical Methods for Data Analysis. Belmont, CA: Wadsworth.

        Variables:
        Ozone: Mean ozone in parts per billion from 1300 to 1500 hours at Roosevelt Island
        Solar.R: Solar radiation in Langleys in the frequency band 4000--7700 Angstroms from 0800 to 1200 hours at Central Park
        Wind: Average wind speed in miles per hour at 0700 and 1000 hours at LaGuardia Airport
        Temp: Maximum daily temperature in degrees Fahrenheit at La Guardia Airport.
        '''.stripIndent()
  }

  static String descCars() {
    '''
        The data give the speed of cars and the distances taken to stop. Note that the data were recorded in the 1920s.
        Original source: Ezekiel, M. (1930) Methods of Correlation Analysis. Wiley.

        Variables:
        speed: the speed in miles per hour
        dist: the stopping distance in feet
        '''.stripIndent()
  }

  static String descMtcars() {
    '''
        The mtcars (Motor Trend Car Road Tests) dataset was extracted from the 1974 Motor Trend US magazine,
        and comprises fuel consumption and 10 aspects of automobile design and performance for 32 automobiles
        (1973–1974 models)

        Variables:
        mpg: Miles/(US) gallon
        cyl: Number of cylinders
        disp: Displacement (cu.in.)
        hp: Gross horsepower
        drat: Rear axle ratio
        wt: Weight (1000 lbs)
        qsec: 1/4 mile time
        vs: V/S
        am: Transmission (0 = automatic, 1 = manual)
        gear: Number of forward gears
        carb: Number of carburetors
        '''.stripIndent()
  }

  static String descIris() {
    '''
        The iris dataset gives the measurements in centimeters of the variables sepal length, sepal width,
        petal length and petal width, respectively, for 50 flowers from each of 3 species of iris.
        The species are Iris setosa, versicolor, and virginica.

        Variables:
        Sepal Length: length of the sepal in cm,
        Sepal Width: width of the sepal in cm,
        Petal Length: length of the petal in cm,
        Petal Width: width of the petal in cm,
        Species: The species of iris i.e. setosa, versicolor, and virginica
        '''.stripIndent()
  }

  static String descPlantGrowth() {
    '''
        The plant growth dataset contains results obtained from an experiment to compare yields
        (as measured by dried weight of plants)
        obtained under a control and two different treatment conditions.

        Variables:
        "": an integer corresponding to a unique observation,
        weight: the dried weight,
        group: ctrl, trt1 or trt2
        '''.stripIndent()
  }

  static String descToothGrowth() {
    '''
        The ToothGrowth data set contains the result from an experiment studying the effect of
        vitamin C on tooth growth in 60 Guinea pigs. Each animal received one of three dose levels of
        vitamin C (0.5, 1, and 2 mg/day) by one of two delivery methods,
        orange juice or ascorbic acid (a form of vitamin C and coded as VC).

        Variables:
        '': an integer corresponding to a unique observation,
        len: Tooth length
        supp: Supplement type (VC or OJ).
        dose: numeric Dose in milligrams/day
        '''.stripIndent()
  }

  static String descUsArrests() {
    '''
        The US arrests data set contains statistics in arrests per 100,000 residents for assault, murder, and rape
        in each of the 50 US states in 1973.

        Variables:
        State: The US state
        Murder: Murder arrests (per 100,000)
        Assault: Assault arrests (per 100,000)
        UrbanPop: Percent urban population
        Rape: Rape arrests (per 100,000)
        '''.stripIndent()
  }

  static String descMpg() {
    '''
        The mpg (miles per gallon) dataset includes information about the fuel economy of popular car models in 1999 and 2008,
        collected by the US Environmental Protection Agency, http://fueleconomy.gov.

        The mpg dataset contains 234 observations of 11 variables:

        Variables:
        manufacturer: manufacturer name
        model: model name, there are 38 models, selected because they had a new edition every year between 1999 and 2008.
        displ: engine displacement, in litres
        year: year of manufacture
        cyl: number of cylinders
        trans: type of transmission
        drv: the type of drive train, where f = front-wheel drive, r = rear wheel drive, 4 = 4wd
        cty: miles per gallon for city driving
        hwy: miles per gallon for highway driving
        fl: fuel type
        class: "type" of car, e.g. two seater, SUV, compact, etc.
        '''.stripIndent()
  }

  static String descDiamonds() {
    '''
        Diamond price and quality information for ~54,000 diamonds obtained from AwesomeGems.com on July 28, 2005

        Variables:
        price: price in US dollars (\\$326--\\$18,823)
        carat: weight of the diamond (0.2--5.01)
        cut: quality of the cut (Fair, Good, Very Good, Premium, Ideal)
        color: diamond colour, from J (worst) to D (best)
        clarity: a measurement of how clear the diamond is (I1 (worst), SI2, SI1, VS2, VS1, VVS2, VVS1, IF (best))
        depth: total depth percentage = z / mean(x, y)
        table: width of top of diamond relative to widest point
        x: length in mm (0--10.74)
        y: width in mm (0--58.9)
        z: depth in mm (0--31.8)
        '''.stripIndent()
  }

  static String descMapData() {
    '''
        Map dataset names:
        county: This database produces a map of the counties of the United States mainland generated from
        the US Department of the Census data

        france: This france database comes from the NUTS III (Tertiary Administrative Units of the European Community)
        database of the United Nations Environment Programme (UNEP) GRID-Geneva data sets (extracted 1989).

        italy: This italy database comes from the NUTS III (Tertiary Administrative Units of the European Community)
        database of the United Nations Environment Programme (UNEP) GRID-Geneva data sets (extracted 1989).

        nz:  The nz database is a New Zealand Basic Map and includes the 3 main Islands and 19 smaller coastal islands.

        state: map of the states of the United States mainland generated from US Department of the Census data

        usa:  map of the United States mainland generated from US Department of the Census data

        world: Low resolution World Map. This world map (from 2013) is imported from the public domain
        Natural Earth project (the 1:50m resolution version)

        world2: Pacific Centric Low resolution World Map. This is an alternative version of the world database
        based on latitudes [0, 360), which then has the Pacific Ocean in the centre of the map.

        Variables:
        long: BigDecimal, the longitude
        lat: BigDecimal, the latitude
        group: Integer, a numeric equivalent to the region
        order: Integer, a sequence number
        region: String, a part of the map area
        subregion: String, a part of the region
        '''.stripIndent()
  }

}
