# Matrix

This is a Groovy library to make it easy to work with
a matrix i.e. a List<List<?>> typically defined in 
Groovy like this `def myList = [ [1,2,3], [3.4, 7.12, 0.19] ]`

Methods are static making is simple to use in Groovy scripts

## Matrix

## Stat
Stat contains basic statistical operations such as sum, mean, median, sd (standard deviation), variance

## DataTable
A DataTable is an immutable Matrix with a header and where each column type is defined.
In some ways you can think of it as an in memory ResultSet.