# AR Measurement

Couldn't find a clean and reliable android AR measurement tool so here it is. Uses ARCore SFM to get planes to fix points to. Optionally uses depth API to instantly place with machine learning estimated depths (not very accurate).

## Use

Move the device until a plane is found, point and place.

- **Distance:** two points.
- **2D Area:** 3 or more points.
- **Reset:** clears the current measurement and placed points.

Only sections of the tracked plane close to crosshair are shown while larger planes are still actually tracked. Planes may be in actually behind a small object if it does not have enough texture to be tracked (solid color surfaces).

## Settings

- **Plane Guide:** shows or hides the current plane gridpoints.
- **Instant Placement:** ML estimated placement without a tracked plane.
- **Depth Occlusion:** optionally hides virtual content behind real surfaces.

## Build

    ./gradlew assembleDebug

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.


ARCore plane detection works best on textured, matte, well lit surfaces while the device is moved
slowly through more than one viewing angle. Don't use this for engineering tasks.
