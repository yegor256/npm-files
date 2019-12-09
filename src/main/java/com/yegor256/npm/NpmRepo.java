package com.yegor256.npm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface NpmRepo {
	
	boolean packageExist(String npmPackageName);
	
	NpmPackage getPackage(String npmPackageName);
	
	class Simple implements NpmRepo {
		
		/**
		 * Dir to use as a package storage.
		 */
		private final Path dir;
		
		public Simple(Path dir) throws IOException {
			this.dir = dir;
			Files.createDirectories(dir);
		}
		
		public Simple() throws IOException {
			this(Files.createTempDirectory("npm-files"));
		}
		
		@Override
		public boolean packageExist(String npmPackageName) {
			return new File(dir.toAbsolutePath().toString() + File.separator + npmPackageName).exists();
		}
		
		@Override
		public NpmPackage getPackage(String npmPackageName) {
			return new NpmPackage(
					new Description(
							Paths.get(dir.toAbsolutePath().toString() + File.separator + npmPackageName)
					)
			);
		}
	}
}
