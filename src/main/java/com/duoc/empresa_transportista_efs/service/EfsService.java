package com.duoc.empresa_transportista_efs.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EfsService {

	@Value("${efs.path}")
	private String efsPath;

	public File saveToEfs(String filename, MultipartFile multipartFile) throws IOException {
		Path dest = resolveFilePath(filename);
		Files.createDirectories(dest.getParent());
		multipartFile.transferTo(dest);
		log.info("Archivo guardado en EFS: {}", dest.toAbsolutePath());
		return dest.toFile();
	}

	public File saveBytes(String filename, byte[] content) throws IOException {
		Path dest = resolveFilePath(filename);
		Files.createDirectories(dest.getParent());
		Files.write(dest, content);
		log.info("Archivo guardado en EFS: {}", dest.toAbsolutePath());
		return dest.toFile();
	}

	public void deleteFile(String filename) throws IOException {
		Path efsRoot = Paths.get(efsPath).normalize().toAbsolutePath();
		List<Path> candidates = resolveCandidatePaths(filename);

		for (Path candidate : candidates) {
			log.info("Intentando eliminar archivo EFS: {}", candidate);
			if (!Files.exists(candidate)) {
				continue;
			}
			Files.delete(candidate);
			if (Files.exists(candidate)) {
				throw new IOException("El archivo sigue existiendo en EFS: " + candidate);
			}
			log.info("Archivo eliminado de EFS: {}", candidate);
			deleteEmptyParents(candidate.getParent(), efsRoot);
			return;
		}

		log.warn("Archivo no encontrado en EFS para key: {} (rutas probadas: {})", filename, candidates);
	}

	private List<Path> resolveCandidatePaths(String filename) {
		String normalized = filename.replace('\\', '/');
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}

		Set<Path> candidates = new LinkedHashSet<>();
		candidates.add(Paths.get(efsPath, normalized.split("/")).normalize());
		candidates.add(Paths.get(efsPath, normalized).normalize());
		candidates.add(Paths.get(efsPath).resolve(normalized).normalize());
		return new ArrayList<>(candidates);
	}

	private Path resolveFilePath(String filename) {
		return resolveCandidatePaths(filename).get(0);
	}

	private void deleteEmptyParents(Path directory, Path efsRoot) throws IOException {
		Path current = directory;
		while (current != null && current.startsWith(efsRoot) && !current.equals(efsRoot)) {
			if (!Files.isDirectory(current) || !isDirectoryEmpty(current)) {
				break;
			}
			Files.delete(current);
			log.info("Carpeta vacia eliminada en EFS: {}", current);
			current = current.getParent();
		}
	}

	private boolean isDirectoryEmpty(Path directory) throws IOException {
		try (var entries = Files.list(directory)) {
			return entries.findAny().isEmpty();
		}
	}
}
