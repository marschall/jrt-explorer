package com.github.marschall.jrtexplorer;


import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class ModulePathData {

    final Path root;
    final Path packages;
    final Path modules;
    final Set<Path> exportedPaths;

    ModulePathData(Path root, Path packages, Path modules, Set<Path> exportedPaths) {
        this.root = root;
        this.packages = packages;
        this.modules = modules;
        this.exportedPaths = exportedPaths;
    }

    static ModulePathData create() {
        FileSystem fileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path root = fileSystem.getPath("/");
        Path packages = fileSystem.getPath("/packages");
        Path modules = fileSystem.getPath("/modules");
        Set<Path> exportedPaths = new HashSet<>();
        for (ModuleReference reference : ModuleFinder.ofInstalled().findAll()) {
            ModuleDescriptor descriptor = reference.descriptor();
            addExportedPaths(exportedPaths, modules, descriptor);
        }
        return new ModulePathData(root, packages, modules, exportedPaths);
    }

    private static void addExportedPaths(Set<Path> exportedPaths, Path modules, ModuleDescriptor descriptor) {
        Path moduleBase = modules.resolve(descriptor.name());
        Set<ModuleDescriptor.Exports> exports = descriptor.exports();
        for (ModuleDescriptor.Exports export : exports) {
            Optional<Set<String>> targets = export.targets();
            if (!targets.isPresent()) {
                String source = export.source();
                Path exportPath = moduleBase.resolve(source.replace('.', '/'));
                while (!exportPath.equals(modules)) {
                    exportedPaths.add(exportPath);
                    exportPath = exportPath.getParent();
                }

            }
        }
    }
}
