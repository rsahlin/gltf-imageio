#gltf-imageio  

Copyright Rickard Sahlin  
This project is licensed under the terms of the MIT license.  

## Key Features  

This project contains various image format related read/write functions.  
It exposes the functionallity to load an image from JPEG, PNG, HDR and KTX2.  
KTX cubemap images can be created from equirectangular (panorama) images.  


- Image utilities  
This package exposes functionality to load image data from JPEG, PNG, HDR and KTX2 files.  
The image data is returned in a native buffer (using the java.nio) and can be integrated with other apis that require pointers to image data.  
Image data can be converted to and from float formats.  

- KTX

This package allows users to create KTX cubemap files from equirectangular (panorama) images.  


- Spherical Harmonics  

This package allows creation of Irradiance map using spherical harmonic coefficients.  
It also let's you visualize how the irradiance coefficients will contribute to diffuse light by drawing it onto the faces of a cubemap.  


## Build Instructions  


The project is built using maven.  
Navigate to the java folder of the source and execute  
'mvn clean install'  

gltf-imageio/java>mvn clean install  

This will build java classes and install in local maven repo so that this project can be accessed by other projects if needed.  
Simply add this project in your projects .pom file:  

```
<dependencies>  
    <dependency>  
        <groupId>com.ikea.digitallabs</groupId>  
        <artifactId>imageio</artifactId>  
        <version>0.0.1-SNAPSHOT</version>  
    </dependency>
</dependencies>          
```
If you are not using maven as a build system you can locate the built .jar file in your local maven repository.  
This is usually located in your user directory, eg c:/user/username/.m2/repository/  
