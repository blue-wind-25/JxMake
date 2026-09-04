public class Real248 {

    private static void copyIfExists(final java.nio.file.Path src, final java.nio.file.Path dst) throws java.io.IOException {
        if( java.nio.file.Files.exists(src) ) {
            java.nio.file.Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
