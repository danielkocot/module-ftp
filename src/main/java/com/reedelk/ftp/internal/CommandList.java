package com.reedelk.ftp.internal;

import com.reedelk.ftp.internal.commons.Default;
import com.reedelk.ftp.internal.commons.Utils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.reedelk.ftp.internal.type.FTPFile.*;
import static com.reedelk.runtime.api.commons.StringUtils.EMPTY;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class CommandList implements Command<List<com.reedelk.ftp.internal.type.FTPFile>> {

    private final String path;
    private final boolean recursive;
    private final boolean filesOnly;
    private final boolean directoriesOnly;

    public CommandList(String path, Boolean recursive, Boolean filesOnly, Boolean directoriesOnly) {
        this.path = path;
        this.recursive = Optional.ofNullable(recursive).orElse(Default.RECURSIVE);
        this.filesOnly = Optional.ofNullable(filesOnly).orElse(Default.FILES_ONLY);
        this.directoriesOnly = Optional.ofNullable(directoriesOnly).orElse(Default.DIRECTORIES_ONLY);
    }

    @Override
    public List<com.reedelk.ftp.internal.type.FTPFile> execute(FTPClient client) throws IOException {
        return recursive ?
                listRecursively(client) :
                list(client);
    }

    private List<com.reedelk.ftp.internal.type.FTPFile> list(FTPClient client) throws IOException {
        FTPFile[] files = client.listFiles(path);
        return stream(files)
                .map(file -> FTPFileWithPath.from(path, file))
                .map(com.reedelk.ftp.internal.type.FTPFile::new)
                .filter(predicates().stream().reduce(x->true, Predicate::and))
                .collect(toList());
    }

    private List<com.reedelk.ftp.internal.type.FTPFile> listRecursively(FTPClient client) throws IOException {
        return listRecursively(client, path, EMPTY)
                .stream()
                .map(com.reedelk.ftp.internal.type.FTPFile::new)
                .filter(predicates().stream().reduce(x->true, Predicate::and))
                .collect(toList());
    }

    private List<FTPFileWithPath> listRecursively(FTPClient client, String parentDir, String currentDir) throws IOException {

        String dirToList = parentDir;
        if (!EMPTY.equals(currentDir)) {
            dirToList += Utils.FTP_PATH_SEPARATOR + currentDir;
        }

        FTPFile[] subFiles = client.listFiles(dirToList);

        List<FTPFileWithPath> allFiles = new ArrayList<>();

        if (subFiles != null && subFiles.length > 0) {

            for (FTPFile currentFile : subFiles) {

                String currentFileName = currentFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and directory itself
                    continue;
                }

                if (currentFile.isDirectory()) {
                    List<FTPFileWithPath> fileWithPaths =
                            listRecursively(client, dirToList, currentFileName);

                    allFiles.add(FTPFileWithPath.from(dirToList, currentFile));
                    allFiles.addAll(fileWithPaths);

                } else {
                    allFiles.add(FTPFileWithPath.from(dirToList, currentFile));
                }
            }
        }
        return allFiles;
    }

    private List<Predicate<Map<String, Serializable>>> predicates() {
        List<Predicate<Map<String, Serializable>>> allPredicates = new ArrayList<>();
        if (filesOnly) allPredicates.add(FILES_ONLY);
        if (directoriesOnly) allPredicates.add(DIRECTORIES_ONLY);
        allPredicates.add(CURRENT_DIRECTORY);
        allPredicates.add(PARENT_DIRECTORY);
        return allPredicates;
    }
}
