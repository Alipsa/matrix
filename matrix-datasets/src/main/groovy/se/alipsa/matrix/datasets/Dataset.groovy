package se.alipsa.matrix.datasets

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.util.FileUtil
import se.alipsa.matrix.core.Stat

// Cannot find matching method java.lang.Object#minus(java.lang.Comparable)
// Need to add more type info for CompileStatic to work
//@groovy.transform.CompileStatic
class Dataset {

  static Matrix airquality() {
    Matrix.builder().data(url('/data/airquality.csv'), ',', '', true)
        .build().convert(
        'Ozone': BigDecimal,
        'Solar.R': BigDecimal,
        'Wind': BigDecimal,
        'Temp': BigDecimal,
        'Month': Short,
        'Day': Short
    ).withMatrixName('airquality')
  }

  static Matrix cars() {
    Matrix.builder().data(url('/data/cars.csv'), ',', '"')
        .build().convert(
        speed: BigDecimal,
        dist: BigDecimal
    ).withMatrixName('cars')
  }

  /**
   * Measurements in centimeters of the variables sepal length, sepal width,
   * petal length and petal width, respectively, for 50 flowers from each of 3 species of iris.
   */
  static Matrix iris() {
    Matrix table = Matrix.builder().data(url('/data/iris.csv'))
        .build()
        .convert(
            Id: Integer,
            'Sepal Length': BigDecimal,
            'Sepal Width': BigDecimal,
            'Petal Length': BigDecimal,
            'Petal Width': BigDecimal,
            Species: String
        )
    table.setMatrixName('Iris')
    return table
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
        .data(url('/data/PlantGrowth.csv'), ',', '"')
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
        .data(url('/data/ToothGrowth.csv'), ',', '"')
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
        .data(url('/data/USArrests.csv'), ',', '"')
        .build()
        .convert(
            "Murder": BigDecimal,
            "Assault": Integer,
            "UrbanPop": Integer,
            "Rape": BigDecimal
        )
  }

  /**
   * Includes information about the fuel economy of popular car models in 1999 and 2008,
   * collected by the US Environmental Protection Agency, http://fueleconomy.gov.
   */
  static Matrix mpg() {
    return Matrix.builder()
        .matrixName('mpg')
        .data(url('/data/mpg.csv'), ',', '"')
        .build()
        .convert(
            "manufacturer": String,
            "model": String,
            "displ": BigDecimal,
            "year": Integer,
            "cyl": Integer,
            "trans": String,
            "drv": String,
            "cty": Integer,
            "hwy": Integer,
            "fl": String,
            "class": String
        )
  }

  static Matrix diamonds() {
    return Matrix.builder()
        .matrixName('diamonds')
        .data(url('/data/diamonds.csv'), ',', '"')
        .build()
        .convert(
            "carat": BigDecimal,
            "cut": String,
            "color": String,
            "clarity": String,
            "depth": BigDecimal,
            "table": BigDecimal,
            "price": Integer,
            "x": BigDecimal,
            "y": BigDecimal,
            "z": BigDecimal
        )
  }

  static Matrix fromUrl(String url, String delimiter = ',', String stringQuote = '') {
    Matrix.builder()
        .data(new URL(url), delimiter, stringQuote)
        .build()
  }

  static Matrix mapData(String datasetName, String region = null, boolean exact = false) {
    if (datasetName == null) {
      throw new IllegalArgumentException("dataset name cannot be null")
    }
    datasetName = datasetName.toLowerCase()
    if (exact && datasetName == 'county' || !exact && datasetName.startsWith('co'))
      return mapDataSet('/data/maps/map_county.csv', region)
    if (exact && datasetName == 'france' || !exact && datasetName.startsWith('fr'))
      return mapDataSet('/data/maps/map_france.csv', region)
    if (exact && datasetName == 'italy' || !exact && datasetName.startsWith('it'))
      return mapDataSet('/data/maps/map_italy.csv', region)
    if (exact && datasetName == 'nz' || !exact && datasetName.startsWith('nz'))
      return mapDataSet('/data/maps/map_nz.csv', region)
    if (exact && datasetName == 'state' || !exact && datasetName.startsWith('st'))
      return mapDataSet('/data/maps/map_state.csv', region)
    if (exact && datasetName == 'usa' || !exact && datasetName.startsWith('us'))
      return mapDataSet('/data/maps/map_usa.csv', region)
    if (datasetName == 'world')
      return mapDataSet('/data/maps/map_world.csv', region)
    if (datasetName == 'world2')
      return mapDataSet('/data/maps/map_world2.csv', region)
    throw new IllegalArgumentException("no map data exists for $datasetName")
  }

  static Matrix mapDataSet(String filePath, String region = null) {
    Matrix ds = Matrix.builder()
        .data(url(filePath), ',', '"')
        .build()
        .convert(
            "long": BigDecimal,
            "lat": BigDecimal,
            "group": Integer,
            "order": Integer,
            "region": String,
            "subregion": String
        )
    if (region == null) {
      return ds
    }
    def sub = ds.subset("region", { it == region })
    def minOrder = Stat.min(sub['order']) - 1
    def minGroup = Stat.min(sub['group']) - 1
    return sub.apply(
        'order', { it - minOrder }
    ).apply(
        'group', { it - minGroup }
    ).orderBy('order')
  }

  private static URL url(String path) {
    FileUtil.getResource(path)
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
  static String describe(String tableName) {
    def name = tableName.toLowerCase()
    if (name == 'airquality') return descAirquality()
    if (name == 'cars') return descCars()
    if (name == 'mtcars') return descMtcars()
    if (name == 'iris') return descIris()
    if (name == 'plantgrowth') return descPlantGrowth()
    if (name == 'toothgrowth') return descToothGrowth()
    if (name == 'usarrests') return descUsArrests()
    if (name == 'mpg') return descMpg()
    if (name == 'diamonds') return descDiamonds()
    if (name == 'mapdata' || name == 'map_data') return descMapData()
    return "Unknown table: ${tableName}"
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
    return '''
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
    return '''
        The iris dataset gives the measurements in centimeters of the variables sepal length, sepal width, 
        petal length and petal width, respectively, for 50 flowers from each of 3 species of iris. 
        The species are Iris setosa, versicolor, and virginica.
        
        Variables:
        Id: an integer corresponding to a unique observation,
        Sepal Length: length of the sepal in cm,
        Sepal Width: width of the sepal in cm,
        Petal Length: length of the petal in cm,
        Petal Width: width of the petal in cm,
        Species: The species of iris i.e. setosa, versicolor, and virginica
        '''.stripIndent()
  }

  static String descPlantGrowth() {
    return '''
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
    return '''
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
    return '''
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
    return '''
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
    return '''
        Diamond price and quality information for ~54,000 diamonds obtained from AwesomeGems.com on July 28, 2005

        Variables:
        price: price in US dollars (\\$326--\\$18,823)
        carat: weight of the diamond (0.2--5.01)
        cut: quality of the cut (Fair, Good, Very Good, Premium, Ideal)
        color: diamond colour, from J (worst) to D (best)
        clarity: a measurement of how clear the diamond is (I1 (worst), SI2, SI1, VS2, VS1, VVS2, VVS1, IF (best))
        x: length in mm (0--10.74)
        y: width in mm (0--58.9)
        z: depth in mm (0--31.8)
        '''.stripIndent()
  }

  static String descMapData() {
    return '''
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