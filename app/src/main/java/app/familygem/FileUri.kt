package app.familygem

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import app.familygem.util.FileUtil
import org.folg.gedcom.model.Media
import java.io.File

/**
 * Manager of local file and URI of a Media.
 * @param fileOnly Searches only for file, not for URI
 */
class FileUri(val context: Context, val media: Media, val treeId: Int = Global.settings.openTree, fileOnly: Boolean = false) {

    private var mediaPath: String? = null
    var file: File? = null
    var uri: Uri? = null
    var path: String? = null
    var name: String? = null
    var extension: String? = null // Always lowercase
    var treeDirFilename = false
    var relative = false
    val fileDescriptor: ParcelFileDescriptor?
        get() = if (file != null) ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        else if (uri != null) context.contentResolver.openFileDescriptor(uri!!, "r")
        else null

    init {
        var normalized = media.file?.replace('\\', '/')

        if (normalized != null) {
            val marker = ".ged.files/"
            val idx = normalized.indexOf(marker)
            if (idx >= 0) {
                normalized = normalized.substring(idx + marker.length)
            }
            media.file = normalized
        }
        mediaPath = normalized

        if (!mediaPath.isNullOrBlank()) {
            file = getFileFromMedia()
            if (!fileOnly && file == null) {
                uri = getUriFromMedia()
            }
            path = file?.absolutePath ?: uri?.path
        }

        name = mediaPath?.substringAfterLast('/')?.substringAfterLast('\\')
            ?: file?.name
                    ?: uri?.let { FileUtil.extractFilename(context, it) }

        extension = name?.let {
            val idx = it.lastIndexOf('.')
            if (idx >= 0) it.substring(idx + 1).lowercase() else null
        }
    }

    /** Looks for the file on the device with different path combinations. */
    private fun getFileFromMedia(): File? {
        val fileName = mediaPath?.substringAfterLast('/') ?: mediaPath ?: return null

        // 1. Application internal storage: $treeId.ged.files + path
        var test = File(File(context.filesDir, "$treeId.ged.files"), mediaPath!!)
        if (test.isFile && test.canRead()) {
            relative = true
            return test
        }

        // 2. Internal storage: filename only
        test = File(File(context.filesDir, "$treeId.ged.files"), fileName)
        if (test.isFile && test.canRead()) {
            if (fileName != mediaPath) treeDirFilename = true
            return test
        }

        // 3. External storage: full path
        test = File(context.getExternalFilesDir(treeId.toString()), mediaPath!!)
        if (test.isFile && test.canRead()) {
            relative = true
            return test
        }

        // 4. External storage: filename only
        test = File(context.getExternalFilesDir(treeId.toString()), fileName)
        if (test.isFile && test.canRead()) {
            if (fileName != mediaPath) treeDirFilename = true
            return test
        }

        // 5. Directories from settings
        for (dir in Global.settings.getTree(treeId).dirs.filterNot { it == null }) {
            test = File(dir, mediaPath!!)
            if (test.isFile && test.canRead()) {
                relative = true
                return test
            }
            test = File(dir, fileName)
            if (test.isFile && test.canRead()) return test
        }

        return null
    }

    /** Looks for the file in the device with any tree-URIs and returns the URI. */
    private fun getUriFromMedia(): Uri? {
        val segments = mediaPath?.split('/') ?: return null
        for (uriStr in Global.settings.getTree(treeId).uris.filterNot { it == null }) {
            var documentDir = DocumentFile.fromTreeUri(context, uriStr.toUri())
            for (segment in segments) {
                val test = documentDir?.findFile(segment)
                if (test?.isFile == true) return test.uri
                else if (test?.isDirectory == true) {
                    relative = true
                    documentDir = test
                } else break
            }
            documentDir?.findFile(segments.last())?.let { if (it.isFile) return it.uri }
        }
        return null
    }

    fun exists(): Boolean = file != null || uri != null

    fun rename(newName: String): Boolean {
        var renamed = false
        if (file != null) {
            renamed = file?.renameTo(File(file?.parentFile, newName)) == true
        } else if (uri != null) {
            DocumentsContract.renameDocument(context.contentResolver, uri!!, newName)?.let { renamed = true }
        }
        return renamed
    }

    fun delete(): Boolean {
        var deleted = false
        if (file != null) {
            deleted = file?.delete() == true
        } else if (uri != null) {
            deleted = DocumentFile.fromSingleUri(context, uri!!)?.delete() == true
        }
        return deleted
    }
}