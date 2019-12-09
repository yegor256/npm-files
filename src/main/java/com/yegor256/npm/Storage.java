package com.yegor256.npm;

import com.jcabi.log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public interface Storage {
	

	boolean exists(String key) throws IOException;
	
	
	void save(String key, Path content) throws IOException;
	
	void load(String key, Path content) throws IOException;
	
	
	final class Simple implements Storage {
	
		private final Path dir;
		
		public Simple() throws IOException {
			this(Files.createTempDirectory("rpm-files"));
		}
		public Simple(final Path path) {
			this.dir = path;
		}
		
		@Override
		public boolean exists(final String key) {
			final Path path = Paths.get(this.dir.toString(), key);
			return Files.exists(path);
		}
		@Override
		public void save(final String key, final Path path) throws IOException {
			final Path target = Paths.get(this.dir.toString(), key);
			target.getParent().toFile().mkdirs();
			Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
			Logger.info(
					this,
					"Saved %d bytes to %s: %s",
					Files.size(target), key, target
			);
		}
		@Override
		public void load(final String key, final Path path) throws IOException {
			final Path source = Paths.get(this.dir.toString(), key);
			Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
			Logger.info(
					this,
					"Loaded %d bytes of %s: %s",
					Files.size(source), key, source
			);
		}
	}
}
