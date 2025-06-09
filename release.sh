#!/bin/bash

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
    exit 1
}

print_info() {
    echo -e "${YELLOW}[i]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    print_error "Please run this script from the project root directory"
fi

# Get current version
CURRENT_VERSION=$(grep "^version = " build.gradle.kts | sed 's/version = "\(.*\)"/\1/')
print_info "Current version: $CURRENT_VERSION"

# Ask for new version
read -p "Enter new version (current: $CURRENT_VERSION): " NEW_VERSION

if [ -z "$NEW_VERSION" ]; then
    print_error "Version cannot be empty"
fi

if [ "$NEW_VERSION" == "$CURRENT_VERSION" ]; then
    print_error "New version must be different from current version"
fi

# Update version in build.gradle.kts
print_status "Updating version in build.gradle.kts..."
sed -i '' "s/version = \"$CURRENT_VERSION\"/version = \"$NEW_VERSION\"/" build.gradle.kts

# Check if there are any uncommitted changes
if ! git diff-index --quiet HEAD --; then
    print_info "You have uncommitted changes. These will be included in the version bump commit."
fi

# Stage all changes
print_status "Staging changes..."
git add .

# Create commit
print_status "Creating commit..."
git commit -m "Bump version to $NEW_VERSION

- Updated version from $CURRENT_VERSION to $NEW_VERSION
- Prepared for plugin marketplace release"

# Clean build directory
print_status "Cleaning build directory..."
./gradlew clean

# Build the plugin
print_status "Building plugin..."
./gradlew buildPlugin

# Run verification
print_status "Running plugin verification..."
./gradlew runPluginVerifier

# Check if build was successful
if [ $? -eq 0 ]; then
    print_status "Build successful!"
    
    # Get the built plugin path
    PLUGIN_FILE="build/distributions/integrant-navigator-${NEW_VERSION}.zip"
    
    if [ -f "$PLUGIN_FILE" ]; then
        print_status "Plugin package created: $PLUGIN_FILE"
        print_info "File size: $(ls -lh "$PLUGIN_FILE" | awk '{print $5}')"
        
        # Create a release directory if it doesn't exist
        mkdir -p releases
        
        # Copy the plugin to releases directory with timestamp
        TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
        RELEASE_FILE="releases/integrant-navigator-${NEW_VERSION}_${TIMESTAMP}.zip"
        cp "$PLUGIN_FILE" "$RELEASE_FILE"
        
        print_status "Release package copied to: $RELEASE_FILE"
        echo ""
        print_status "Release preparation complete!"
        echo ""
        echo "Next steps:"
        echo "1. Test the plugin locally: Install from disk using $PLUGIN_FILE"
        echo "2. Create a git tag: git tag v$NEW_VERSION"
        echo "3. Push changes: git push && git push --tags"
        echo "4. Upload to JetBrains Marketplace: https://plugins.jetbrains.com/plugin/add"
        echo "   - Use file: $PLUGIN_FILE"
    else
        print_error "Plugin file not found at expected location"
    fi
else
    print_error "Build failed! Check the output above for errors."
fi