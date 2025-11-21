# Packaging Optimization Guide

## Summary

✅ **Good News**: The `data/` and `models/` directories are **NOT included** in the JAR package.

⚠️ **Issue**: The JAR file is still **1.6GB** due to DeepLearning4j native libraries for all platforms.

## What's Taking Up Space

The large JAR size (1.6GB) is primarily due to ND4J (DeepLearning4j's numerical computing library) including native binaries for **all platforms**:

| Library | Size | Platforms |
|---------|------|-----------|
| nd4j-native | ~400MB | Windows x86_64, Linux x86_64, macOS x86_64, macOS ARM64 |
| nd4j-native-android | ~300MB | Android ARM, ARM64, x86, x86_64 |
| opencv | ~200MB | Multiple platforms |
| openblas | ~100MB | Multiple platforms |

**Total native libs**: ~1.5GB out of 1.6GB JAR

## Files Excluded from Packaging

### 1. `.gitignore` (Updated)
```gitignore
# Exclude heavy data and model files
/data/
/models/
*.ser
*.zip
data/models/**
data/bhavcopy/**
data/logs/**
```

### 2. `.dockerignore` (Created)
```dockerignore
# Exclude heavy data and model files from Docker builds
data/
models/
*.ser
*.zip
target/
.idea/
*.md
*.pdf
src/test/
logs/
```

### 3. `pom.xml` Maven Resources Plugin (Updated)
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <configuration>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <excludes>
                    <exclude>data/**</exclude>
                    <exclude>models/**</exclude>
                </excludes>
            </resource>
        </resources>
    </configuration>
</plugin>
```

## Options to Reduce JAR Size

### Option 1: Use Platform-Specific ND4J (Recommended for Production)

Replace the platform-agnostic dependency with platform-specific ones:

**For Linux x86_64 (most cloud servers):**
```xml
<!-- Replace nd4j-native-platform with platform-specific version -->
<dependency>
    <groupId>org.nd4j</groupId>
    <artifactId>nd4j-native</artifactId>
    <version>${dl4j.version}</version>
</dependency>
<dependency>
    <groupId>org.nd4j</groupId>
    <artifactId>nd4j-native</artifactId>
    <version>${dl4j.version}</version>
    <classifier>linux-x86_64</classifier>
</dependency>
```

**Expected JAR size**: ~300MB (saves ~1.3GB!)

### Option 2: Make DL4J Dependencies Optional

If LSTM model training is not critical for production:

```xml
<dependency>
    <groupId>org.deeplearning4j</groupId>
    <artifactId>deeplearning4j-core</artifactId>
    <version>${dl4j.version}</version>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.nd4j</groupId>
    <artifactId>nd4j-native-platform</artifactId>
    <version>${dl4j.version}</version>
    <optional>true</optional>
</dependency>
```

Then use Maven profiles to include/exclude:
```bash
# Build without ML features (small JAR)
mvn clean package -P no-ml

# Build with ML features (large JAR)
mvn clean package -P with-ml
```

### Option 3: Separate the ML Module

Create two deployable artifacts:
1. **Main Application** (~100MB) - Stock analysis without ML
2. **ML Service** (~1.6GB) - Separate microservice for LSTM predictions

### Option 4: External Model Storage

Keep trained models outside the JAR:
- Store models in S3, Azure Blob, or local filesystem
- Load models at runtime from external storage
- Much smaller deployment artifact

## Verification

To verify what's in your JAR:
```bash
# Check JAR size
ls -lh target/stock-analyzer-1.0.0.jar

# Verify data/ and models/ are NOT included
unzip -l target/stock-analyzer-1.0.0.jar | grep -E "(data/|models/)"

# Check largest dependencies
unzip -l target/stock-analyzer-1.0.0.jar | sort -k4 -n | tail -20
```

## Recommended Production Setup

For cloud deployment (AWS, Azure, GCP):

1. **Use platform-specific ND4J** (Option 1) → ~300MB JAR
2. **Add `.dockerignore`** to exclude data from Docker images
3. **Use external volume/storage** for:
   - Trained models (`/data/models/`)
   - Bhavcopy data (`/data/bhavcopy/`)
   - Logs (`/data/logs/`)
4. **Docker volume mount**:
   ```bash
   docker run -v /host/data:/app/data stock-analyzer
   ```

## Current Configuration Status

✅ `data/` and `models/` directories **excluded from Git** (.gitignore)  
✅ `data/` and `models/` directories **excluded from Docker** (.dockerignore)  
✅ `data/` and `models/` directories **NOT in JAR** (verified)  
⚠️ JAR still large (1.6GB) due to ND4J multi-platform natives  
💡 **Next step**: Implement Option 1 for production deployment

## Implementation Guide for Option 1

1. Edit `pom.xml` and replace:
   ```xml
   <!-- Current (includes all platforms) -->
   <dependency>
       <groupId>org.nd4j</groupId>
       <artifactId>nd4j-native-platform</artifactId>
       <version>${dl4j.version}</version>
   </dependency>
   ```

   With:
   ```xml
   <!-- Platform-specific (Linux x86_64 only) -->
   <dependency>
       <groupId>org.nd4j</groupId>
       <artifactId>nd4j-native</artifactId>
       <version>${dl4j.version}</version>
   </dependency>
   <dependency>
       <groupId>org.nd4j</groupId>
       <artifactId>nd4j-native</artifactId>
       <version>${dl4j.version}</version>
       <classifier>linux-x86_64</classifier>
   </dependency>
   ```

2. Rebuild:
   ```bash
   mvn clean package -DskipTests
   ```

3. Verify size reduction:
   ```bash
   ls -lh target/*.jar
   ```

**Result**: JAR size should drop from 1.6GB to ~250-300MB! 🎉

