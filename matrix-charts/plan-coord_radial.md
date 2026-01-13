implementation plan for coord_radial(). The plan covers:

Key Features to Implement:
- Partial circles via start/end parameters (sectors, semicircles)
- Inner radius for donut-style visualizations
- Automatic text rotation with rotateAngle
- Direction control (clockwise/anticlockwise)
- Integration with the guide system

Files to Create/Modify:
1. CoordRadial.groovy - New coordinate class
2. GgPlot.groovy - Add factory method
3. GgRenderer.groovy - Handle plot dimensions
4. GridRenderer.groovy - Radial grid (arcs + spokes)
5. AxisRenderer.groovy - Radial axis labels
6. CoordRadialTest.groovy - Comprehensive tests

Sources:
- https://tidyverse.org/blog/2024/03/ggplot2-3-5-0-coord-radial/
- https://github.com/tidyverse/ggplot2/blob/main/R/coord-radial.R

● User approved Claude's plan 

Implementation Plan: coord_radial()

     Overview                                                                                                                                                     
     Implement coord_radial() for the matrix-charts gg module, matching R's ggplot2 behavior. This is a modern alternative to coord_polar() introduced in ggplot2 3.5.0 that adds support     
     for partial circles (sectors), inner radius (donut plots), and better integration with the guide system.             

     R's ggplot2 API Reference

     coord_radial(             
       theta = "x",              
       start = 0,                
       end = NULL,                
       expand = TRUE,                       
       direction = 1,                         
       clip = "off",                       
       r.axis.inside = NULL,                                               
       rotate.angle = FALSE,                                                
       inner.radius = 0                                                           
     )  

     Key Parameters                                                                                                       
     ┌───────────────┬─────────┬─────────┬───────────────────────────────────────────────┐ 
     │   Parameter   │  Type   │ Default │                  Description                  │ 
     ├───────────────┼─────────┼─────────┼───────────────────────────────────────────────┤
     │ theta         │ string  │ "x"     │ Which variable maps to angle ("x" or "y")     │ 
     ├───────────────┼─────────┼─────────┼───────────────────────────────────────────────┤
     │ start         │ numeric │ 0       │ Start angle in radians (0 = 12 o'clock)       │
     ├───────────────┼─────────┼─────────┼───────────────────────────────────────────────┤ 
     │ end           │ numeric │ NULL    │ End angle in radians (NULL = start + 2π)      │ 
     ├───────────────┼─────────┼─────────┼───────────────────────────────────────────────┤  
     │ expand        │ boolean │ TRUE    │ Add expansion factor to limits                │
     ├───────────────┼─────────┼─────────┼───────────────────────────────────────────────┤
     │ direction     │ integer │ 1       │ 1 = clockwise, -1 = anticlockwise             │ 
     ├───────────────┼─────────┼─────────┼───────────────────────────────────────────────┤
     │ clip          │ string  │ "off"   │ Clipping behavior ("on" or "off")             │ 
     ├───────────────┼─────────┼─────────┼───────────────────────────────────────────────┤ 
     │ r.axis.inside │ boolean │ NULL    │ Position r-axis inside plot                   │ 
     ├───────────────┼─────────┼─────────┼───────────────────────────────────────────────┤
     │ rotate.angle  │ boolean │ FALSE   │ Auto-rotate text with theta                   │ 
     ├───────────────┼─────────┼─────────┼───────────────────────────────────────────────┤
     │ inner.radius  │ numeric │ 0       │ Inner radius ratio (0-1, 0 = pie, >0 = donut) │  
     └───────────────┴─────────┴─────────┴───────────────────────────────────────────────┘                                                                                                    
     Key Differences from coord_polar()  

     1. Partial circles: start/end parameters allow sectors (semicircles, quarter circles)                
     2. Inner radius: Creates donut-style visualizations         
     3. Text rotation: rotate.angle auto-aligns text tangent to circle       
     4. Guide integration: Uses guide_axis_theta() for theta axis     
     5. Expansion handling: Similar to coord_cartesian() rather than coord_polar()  

     Implementation Approach    

     Create CoordRadial extending the base Coord class, following the CoordPolar pattern but adding the new features. Update renderers to handle the radial coordinate system.                
                                                                                                                                                                                              
     Files to Create/Modify   

     1. Create: CoordRadial.groovy                                                                                                                                                         
     Path: matrix-charts/src/main/groovy/se/alipsa/matrix/gg/coord/CoordRadial.groovy                                                                            
     @CompileStatic                            
     class CoordRadial extends Coord {                                 
       // Which variable maps to angle                                            
       String theta = 'x' 

       // Angular range (radians)
       BigDecimal start = 0
       BigDecimal end = null  // null = start + 2π (full circle) 

       // Direction: true = clockwise (direction=1), false = anticlockwise (direction=-1)                                 
       boolean clockwise = true 

       // Inner radius ratio (0-1): 0 = full pie, >0 = donut                                          
       BigDecimal innerRadius = 0                                                                                         

       // Expansion and clipping                                              
       boolean expand = true                                        
       boolean clip = false  // "off" by default in coord_radial                                                          

       // Text rotation for labels               
       boolean rotateAngle = false                                                                                        

       // R-axis positioning                                        
       Boolean rAxisInside = null                                                                                         

       // Plot dimensions (set by renderer)                                            
       BigDecimal plotWidth = 640                                    
       BigDecimal plotHeight = 480  

       // Flag for type checking in renderers                            
       final boolean radial = true       
       // Constructors, transform(), inverse(), helper methods...                                    
     } 

     Key Methods:                                                                                                                                                                             
     - transform(Number x, Number y, Map scales) - Convert data to pixel coords  
     - inverse(Number px, Number py, Map scales) - Convert pixel to data coords     
     - getCenter() - Return plot center point                  
     - getMaxRadius() - Return maximum outer radius                 
     - getInnerRadiusPx() - Return inner radius in pixels      
     - getAngularRange() - Return [startAngle, endAngle] in radians     
     - createArcPath(...) - Generate SVG arc path for sectors
     - getTextRotation(Number theta) - Calculate text rotation angle   

     2. Modify: GgPlot.groovy 

     Path: matrix-charts/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy 

     Add factory methods:

     /**          
      * Modern radial coordinate system for circular plots.             
      *                   
      * Unlike coord_polar(), coord_radial() supports:                    
      * - Partial circles via start/end parameters                       
      * - Inner radius for donut-style plots                    
      * - Automatic text rotation with rotate.angle              
      *
      * @param params optional parameters (theta, start, end, innerRadius, etc.)                                   
      * @return CoordRadial instance                                                         
      */                                                    
     static CoordRadial coord_radial(Map params = [:]) {                                       
       return new CoordRadial(params)
     }  

     3. Modify: GgRenderer.groovy  

     Path: matrix-charts/src/main/groovy/se/alipsa/matrix/gg/render/GgRenderer.groovy                                     

     Add CoordRadial handling alongside CoordPolar:                                                                       

     // In render() method, add branch for CoordRadial                                   
     if (coord instanceof CoordRadial) {                                    
       coord.plotWidth = effectiveWidth                                      
       coord.plotHeight = effectiveHeight                                       
     }                                                                                                                    

     4. Modify: GridRenderer.groovy                                                                                       

     Path: matrix-charts/src/main/groovy/se/alipsa/matrix/gg/render/GridRenderer.groovy                                   

     Add radial grid rendering (concentric arcs + radial spokes):                                                                                                                              
     if (coord instanceof CoordRadial) {                               
       renderRadialGrid(plotArea, scales, theme, coord as CoordRadial)   
       return                                                                  
     }            

     private void renderRadialGrid(G plotArea, Map scales, Theme theme, CoordRadial coord) {      
       // Draw concentric arcs (partial circles based on start/end)  
       // Draw radial spokes from center (or inner radius) to outer edge                   
       // Handle inner radius for donut-style grids                          
     }    

     5. Modify: AxisRenderer.groovy                                                                                                                        
     Path: matrix-charts/src/main/groovy/se/alipsa/matrix/gg/render/AxisRenderer.groovy   

     Add radial axis rendering:                                                                                           

     if (coord instanceof CoordRadial) {                                
       renderRadialAxes(plotArea, scales, theme, coord as CoordRadial)
       return                                                                      
     }                                                                                                                   

     private void renderRadialAxes(G plotArea, Map scales, Theme theme, CoordRadial coord) {            
       // Render theta axis (angular labels around the arc)                  
       // Render r axis (radial labels, optionally inside)                   
       // Handle text rotation if rotateAngle is true                         
       // Respect start/end for partial circles                             
     }                                                                                                                                                                 
     6. Create: CoordRadialTest.groovy                                                                                                                                         
     Path: matrix-charts/src/test/groovy/gg/CoordRadialTest.groovy                                                                                                        
     Test cases:                                                                                                                                                                              
     1. Default parameters create full circle                              
     2. Partial circle with start/end                                    
     3. Inner radius creates donut                                       
     4. Direction (clockwise vs anticlockwise)                                 
     5. Text rotation calculation                                         
     6. Transform/inverse round-trip                                     
     7. Rendering with various geoms (point, bar, line)                   
     8. Edge cases (empty data, single point, theta='y')                                                                                                            
     Implementation Details                                                                                                                                      
     Angular Range Calculation 

     List<BigDecimal> getAngularRange() {         
       BigDecimal effectiveEnd = (end != null) ? end : (start + 2 * Math.PI)                                    
       return [start, effectiveEnd]                                      
     }                        

     BigDecimal getAngularSpan() {                                     
       def range = getAngularRange()                                   
       return (range[1] - range[0]).abs()                                     
     }                                                                                                                                                                   
     Transform with Inner Radius                                                                                                                              
     @CompileDynamic                                 
     List<Number> transform(Number x, Number y, Map<String, ?> scales) {                                                                                                                      
       // Normalize values to 0-1                           
       Number thetaNorm = theta == 'y' ? normalize(y, scales['y']) : normalize(x, scales['x'])    
       Number rNorm = theta == 'y' ? normalize(x, scales['x']) : normalize(y, scales['y'])  

       // Calculate angle within the angular range                         
       def angularRange = getAngularRange()      
       BigDecimal angle = angularRange[0] + thetaNorm * getAngularSpan()                                                                                                                      
       if (!clockwise) {                    
         angle = angularRange[0] - thetaNorm * getAngularSpan()                                                         
       }  

       // Calculate radius with inner radius offset                                                                       
       BigDecimal maxR = getMaxRadius()                                                                                   
       BigDecimal minR = innerRadius * maxR                                                                    
       BigDecimal radius = minR + rNorm * (maxR - minR)                                                                   

       // Convert to Cartesian pixel coordinates                                                                          
       def center = getCenter()                                                                                           
       BigDecimal px = center[0] + radius * Math.sin(angle)                                                               
       BigDecimal py = center[1] - radius * Math.cos(angle)                                                               
       return [px, py]                                                                                                    
     }                                                                                                                                                                                                                                         
     Text Rotation                                                                                                                                                                    
     /**                                                                                                                  
      * Calculate text rotation angle for a given theta position.                                                         
      * When rotateAngle is true:                                                                                         
      * - 0 degrees places text tangent to the circle                                                                     
      * - 90 degrees places text along the radius                                                                         
      */                                                                                                                                                                                      
     BigDecimal getTextRotation(Number thetaNorm) {                                                                       
       if (!rotateAngle) return 0                                                                                         
       def angularRange = getAngularRange()                                                                               
       BigDecimal angle = angularRange[0] + thetaNorm * getAngularSpan()                                                                                                                      
       if (!clockwise) {                                                                                                  
         angle = angularRange[0] - thetaNorm * getAngularSpan()                                                           
       }                                                                                                                                                                           
       // Convert to degrees and adjust for text orientation                                                              
       BigDecimal degrees = angle.toDegrees()                                                                         
       // Adjust so text reads from left to right                                                                         
       if (degrees > 90 && degrees < 270) {                                                                               
         degrees += 180                                                                                                    
       }                                                                                                                  
       return degrees                                                                                                     
     }                                                                                                                                                                   
     Arc Path for Partial Circles                                                                                                                                                        
     String createArcPath(double startAngle, Number endAngle, Number innerR, Number outerR) {                             
       def center = getCenter()                                                                                           
       BigDecimal cx = center[0]                                                                                          
       BigDecimal cy = center[1]                                                                                                                                             
       // Clamp angles to the coordinate's angular range                                                                  
       def range = getAngularRange()                                                                                      
       startAngle = startAngle.max(range[0])                                                              
       endAngle = endAngle.min(range[1])                                                                  
       // Calculate arc endpoints...                                                                                      
       // Build SVG path with proper arc flags...                                                                         
     }                                                                                                                                                                            
     Verification                                                                                                                                                                   
     1. Unit tests: Run ./gradlew :matrix-charts:test --tests "gg.CoordRadialTest"                                        
     2. Manual verification: Create test charts:                                                                                                                                              
     // Full radial plot                                                                                                  
     ggplot(data, aes('x', 'y')) + geom_point() + coord_radial()                                                                                                     
     // Semicircle                                                                                                        
     ggplot(data, aes('x', 'y')) + geom_bar() +                                                                           
       coord_radial(start: -Math.PI/2, end: Math.PI/2)                                                                                                                            
     // Donut chart                                                                                                       
     ggplot(data, aes(x: '', y: 'value', fill: 'category')) +                                                             
       geom_bar(stat: 'identity') +                                                                                        
       coord_radial(theta: 'y', innerRadius: 0.3)                                                                                                                                             
     3. SVG validation: Use SvgWriter.toXml() for assertions                                                              
     4. Comparison with coord_polar: Verify similar behavior for equivalent parameters                                                                                    
     Update ggTodo.md                                                                                                                                                    
     Mark item 4.9.2 as complete:                                                                                         
     - 4.9.2 [x] `coord_radial()` - Modern polar alternative                                                              
                                                            
     References                                                                                                           
     - https://tidyverse.org/blog/2024/03/ggplot2-3-5-0-coord-radial/                                                     
     - https://github.com/tidyverse/ggplot2/blob/main/R/coord-radial.R                                                    
     - Existing pattern: CoordPolar.groovy                                                                                                                                                    

● The implementation plan for coord_radial() has been written and approved. The plan is saved at /home/per/.claude/plans/floofy-imagining-anchor.md.

Summary of the plan:                                                                                                                                                                        
┌────────────────────────┬───────────────────────────────────────────────────────────────────────────────┐                
│       Component        │                                    Action                                     │                
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤                
│ CoordRadial.groovy     │ Create new coordinate class with partial circles, inner radius, text rotation │                
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤                
│ GgPlot.groovy          │ Add coord_radial(Map params) factory method                                   │                
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤                
│ GgRenderer.groovy      │ Add CoordRadial dimension handling                                            │                
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤                
│ GridRenderer.groovy    │ Add radial grid rendering (arcs + spokes)                                     │                
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤                
│ AxisRenderer.groovy    │ Add radial axis labels with rotation support                                  │                
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤                
│ CoordRadialTest.groovy │ Comprehensive test suite                                                      │                 
└────────────────────────┴───────────────────────────────────────────────────────────────────────────────┘                                                                                  
Key features to implement:
- Partial circles via start/end parameters
- Inner radius for donut-style plots (innerRadius: 0.3)
- Automatic text rotation (rotateAngle: true)
- Direction control (clockwise/anticlockwise)   