# These examples are from the '6 Maps' chapter of the Ggplot2 Book (https://ggplot2-book.org/maps.html)
library(ggplot2)

# 6.1 Polygon maps

mi_counties <- map_data("county", "michigan") %>%
  select(lon = long, lat, group, id = subregion)
head(mi_counties)
#>     lon  lat group     id
#> 1 -83.9 44.9     1 alcona
#> 2 -83.4 44.9     1 alcona
#> 3 -83.4 44.9     1 alcona
#> 4 -83.3 44.8     1 alcona
#> 5 -83.3 44.8     1 alcona
#> 6 -83.3 44.8     1 alcona

ggplot(mi_counties, aes(lon, lat)) +
  geom_point(size = .25, show.legend = FALSE) +
  coord_quickmap()

ggplot(mi_counties, aes(lon, lat, group = group)) +
  geom_polygon(fill = "white", colour = "grey50") +
  coord_quickmap()


# 6.2 Simple features maps

library(ozmaps)
library(sf)
#> Linking to GEOS 3.8.0, GDAL 3.0.4, PROJ 6.3.1; sf_use_s2() is TRUE

oz_states <- ozmaps::ozmap_states
oz_states
#> Simple feature collection with 9 features and 1 field
#> Geometry type: MULTIPOLYGON
#> Dimension:     XY
#> Bounding box:  xmin: 106 ymin: -43.6 xmax: 168 ymax: -9.23
#> Geodetic CRS:  GDA94
#> # A tibble: 9 × 2
#>   NAME                                                                  geometry
#> * <chr>                                                       <MULTIPOLYGON [°]>
#> 1 New South Wales   (((151 -35.1, 151 -35.1, 151 -35.1, 151 -35.1, 151 -35.2, 1…
#> 2 Victoria          (((147 -38.7, 147 -38.7, 147 -38.7, 147 -38.7, 147 -38.7)),…
#> 3 Queensland        (((149 -20.3, 149 -20.4, 149 -20.4, 149 -20.3)), ((149 -20.…
#> 4 South Australia   (((137 -34.5, 137 -34.5, 137 -34.5, 137 -34.5, 137 -34.5, 1…
#> 5 Western Australia (((126 -14, 126 -14, 126 -14, 126 -14, 126 -14)), ((124 -16…
#> 6 Tasmania          (((148 -40.3, 148 -40.3, 148 -40.3, 148 -40.3)), ((147 -39.…
#> # … with 3 more rows

ggplot(oz_states) +
  geom_sf() +
  coord_sf()


# 6.2.1 Layered maps

oz_states <- ozmaps::ozmap_states %>% filter(NAME != "Other Territories")
oz_votes <- rmapshaper::ms_simplify(ozmaps::abs_ced)
#> Registered S3 method overwritten by 'geojsonlint':
#>   method         from
#>   print.location dplyr

ggplot() +
  geom_sf(data = oz_states, mapping = aes(fill = NAME), show.legend = FALSE) +
  geom_sf(data = oz_votes, fill = NA) +
  coord_sf()


# 6.2.2 Labelled maps

# Filter electorates in the Sydney metropolitan region
sydney_map <- ozmaps::abs_ced %>% filter(NAME %in% c(
  "Sydney", "Wentworth", "Warringah", "Kingsford Smith", "Grayndler", "Lowe",
  "North Sydney", "Barton", "Bradfield", "Banks", "Blaxland", "Reid",
  "Watson", "Fowler", "Werriwa", "Prospect", "Parramatta", "Bennelong",
  "Mackellar", "Greenway", "Mitchell", "Chifley", "McMahon"
))

# Draw the electoral map of Sydney
ggplot(sydney_map) +
  geom_sf(aes(fill = NAME), show.legend = FALSE) +
  coord_sf(xlim = c(150.97, 151.3), ylim = c(-33.98, -33.79)) +
  geom_sf_label(aes(label = NAME), label.padding = unit(1, "mm"))
#> Warning in st_point_on_surface.sfc(sf::st_zm(x)): st_point_on_surface may not
#> give correct results for longitude/latitude data


# 6.2.3 Adding other geoms

oz_capitals <- tibble::tribble(
  ~city,           ~lat,     ~lon,
  "Sydney",    -33.8688, 151.2093,
  "Melbourne", -37.8136, 144.9631,
  "Brisbane",  -27.4698, 153.0251,
  "Adelaide",  -34.9285, 138.6007,
  "Perth",     -31.9505, 115.8605,
  "Hobart",    -42.8821, 147.3272,
  "Canberra",  -35.2809, 149.1300,
  "Darwin",    -12.4634, 130.8456,
)

ggplot() +
  geom_sf(data = oz_votes) +
  geom_sf(data = oz_states, colour = "black", fill = NA) +
  geom_point(data = oz_capitals, mapping = aes(x = lon, y = lat), colour = "red") +
  coord_sf()


# 6.3 Map projections

st_crs(oz_votes)
#> Coordinate Reference System:
#>   User input: EPSG:4283
#>   wkt:
#> GEOGCRS["GDA94",
#>     DATUM["Geocentric Datum of Australia 1994",
#>         ELLIPSOID["GRS 1980",6378137,298.257222101,
#>             LENGTHUNIT["metre",1]]],
#>     PRIMEM["Greenwich",0,
#>         ANGLEUNIT["degree",0.0174532925199433]],
#>     CS[ellipsoidal,2],
#>         AXIS["geodetic latitude (Lat)",north,
#>             ORDER[1],
#>             ANGLEUNIT["degree",0.0174532925199433]],
#>         AXIS["geodetic longitude (Lon)",east,
#>             ORDER[2],
#>             ANGLEUNIT["degree",0.0174532925199433]],
#>     USAGE[
#>         SCOPE["Horizontal component of 3D system."],
#>         AREA["Australia including Lord Howe Island, Macquarie Islands, Ashmore and Cartier Islands, Christmas Island, Cocos (Keeling) Islands, Norfolk Island. All onshore and offshore."],
#>         BBOX[-60.56,93.41,-8.47,173.35]],
#>     ID["EPSG",4283]]

st_crs(oz_votes) == st_crs(4283)
#> [1] TRUE

ggplot(oz_votes) + geom_sf()
ggplot(oz_votes) + geom_sf() + coord_sf(crs = st_crs(3112))


# 6.4 Working with sf data

edenmonaro <- ozmaps::abs_ced %>% filter(NAME == "Eden-Monaro")

p <- ggplot(edenmonaro) + geom_sf()
p + coord_sf(xlim = c(147.75, 150.25), ylim = c(-37.5, -34.5))
p + coord_sf(xlim = c(150, 150.25), ylim = c(-36.3, -36))

edenmonaro <- edenmonaro %>% pull(geometry)