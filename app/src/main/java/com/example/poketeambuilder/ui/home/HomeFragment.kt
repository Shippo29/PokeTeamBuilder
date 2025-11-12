package com.example.poketeambuilder.ui.home

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.gridlayout.widget.GridLayout
import coil.load
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
// No usamos ActivityResultLauncher para guardar imágenes
import androidx.lifecycle.lifecycleScope
import com.example.poketeambuilder.R
import com.google.android.material.button.MaterialButton
import coil.Coil
import android.net.Uri
import android.util.Log
import coil.request.ImageRequest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    // Código de request para permisos
    private val REQUEST_PERMISSIONS = 1001

    // Usamos requestPermissions() y onRequestPermissionsResult

    // En Android Q+ no se requieren permisos para guardar en MediaStore,
    // pero en versiones antiguas solicitamos permisos para escribir en Pictures/TuApp.

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    // No hay registros que realizar aquí; usaremos requestPermissions() desde el diálogo
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    val btnSelect = view.findViewById<MaterialButton>(R.id.btn_select_team)
    val btnDownload = view.findViewById<MaterialButton>(R.id.btn_download_team)
        val btnSearch = view.findViewById<MaterialButton>(R.id.btn_advanced_search)
        val btnSettings = view.findViewById<ImageButton>(R.id.btn_settings)
    val cardSelected = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_selected_team)
    val homeGrid = view.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.home_team_grid)
    val tvSelectedTitle = view.findViewById<TextView>(R.id.tv_selected_team_title)

        btnSelect.setOnClickListener {
            // Abrir diálogo de lista de equipos
            val dlg = com.example.poketeambuilder.ui.home.TeamsListDialog()
            dlg.show(requireActivity().supportFragmentManager, "teams_list")
        }

        btnDownload.setOnClickListener {
            // Mostrar modal explicando por qué necesitamos acceso a la galería y luego solicitar permisos
            showPermissionPreDialog()
        }
        btnSearch.setOnClickListener {
            // Abrir Pokédex (Búsqueda avanzada)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, com.example.poketeambuilder.ui.pokedex.PokedexFragment())
                .addToBackStack(null)
                .commit()
        }
        btnSettings.setOnClickListener {
            val options = arrayOf("Gestionar equipos", "Eliminar equipo actual")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Gestión de equipos")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // abrir lista de equipos
                            val dlg = com.example.poketeambuilder.ui.home.TeamsListDialog()
                            dlg.show(requireActivity().supportFragmentManager, "teams_list")
                        }
                        1 -> {
                            val sel = com.example.poketeambuilder.data.teams.TeamManager.getSelectedTeam(requireContext())
                            if (sel == null) {
                                Toast.makeText(requireContext(), "No hay equipo seleccionado", Toast.LENGTH_SHORT).show()
                                return@setItems
                            }
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Eliminar equipo")
                                .setMessage("¿Estás seguro de que deseas eliminar \"${sel.name}\"?\nEsta acción no se puede deshacer.")
                                .setPositiveButton("Eliminar") { _, _ ->
                                    lifecycleScope.launch {
                                        val ok = com.example.poketeambuilder.data.teams.TeamManager.deleteTeam(requireContext(), sel.id)
                                        if (ok) {
                                            Toast.makeText(requireContext(), "Equipo eliminado", Toast.LENGTH_SHORT).show()
                                            refreshSelectedTeam(homeGrid, tvSelectedTitle)
                                        } else {
                                            Toast.makeText(requireContext(), "No se pudo eliminar el equipo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    }
                }
                .show()
        }

    // La imagen de encabezado fue eliminada; el equipo seleccionado es ahora el elemento principal

    // Cargamos el Pokémon destacado
        viewModel.loadFeaturedPokemon(25)

    // Iniciar transiciones cuando la vista esté lista
        view.doOnPreDraw { startPostponedEnterTransition() }

    // Rellenar vista previa del equipo seleccionado (si existe)
        refreshSelectedTeam(homeGrid, tvSelectedTitle)

    // Escuchar selección de equipo desde dialogos/fragmentos para refrescar la UI
        parentFragmentManager.setFragmentResultListener("team_selected", viewLifecycleOwner) { _, bundle ->
            val homeGridLocal = view.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.home_team_grid)
            val tvSelectedLocal = view.findViewById<TextView>(R.id.tv_selected_team_title)
            refreshSelectedTeam(homeGridLocal, tvSelectedLocal)
        }
    }

    private fun requiredPermissionsForDevice(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun showPermissionPreDialog() {
        val msg = "La aplicación necesita permiso para guardar archivos en tu dispositivo. ¿Deseas otorgar permiso?"
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage(msg)
            .setPositiveButton("Permitir") { _, _ ->
                // Solicitar permisos de lectura/escritura al sistema (y galería en Android 13+)
                val perms = arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
                requestPermissions(perms, REQUEST_PERMISSIONS)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Toast.makeText(requireContext(), "No se puede descargar la foto sin permitir guardar archivos en el dispositivo.", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            // Comprobar si WRITE y READ (o READ_MEDIA_IMAGES) fueron concedidos
            val grantedAll = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (grantedAll) {
                // ejecutar la descarga automáticamente
                view?.let { v ->
                    val cardSelected = v.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_selected_team)
                    lifecycleScope.launch {
                        val tag = "HomeFragment.download"
                        try {
                            val bitmap = captureViewBitmap(cardSelected)
                            Log.d(tag, "Captured bitmap: ${bitmap.width}x${bitmap.height}")
                            if (bitmap.width <= 1 || bitmap.height <= 1) {
                                Toast.makeText(requireContext(), "No se pudo capturar la imagen del equipo (vista no lista)", Toast.LENGTH_LONG).show()
                                Log.e(tag, "Bitmap has invalid size after capture")
                                return@launch
                            }
                            val saved = saveBitmapToGallery(bitmap, "poketeam_${System.currentTimeMillis()}.png")
                            if (saved) {
                                Toast.makeText(requireContext(), "Imagen guardada en la galería", Toast.LENGTH_SHORT).show()
                                Log.d(tag, "Image saved successfully")
                            } else {
                                Toast.makeText(requireContext(), "Error al guardar imagen", Toast.LENGTH_SHORT).show()
                                Log.e(tag, "saveBitmapToGallery returned false")
                            }
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error al generar imagen: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("HomeFragment.download", "Exception while saving image", e)
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "No se puede descargar la foto sin permitir guardar archivos en el dispositivo.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
    // refrescar por si cambió la selección
        view?.let { v ->
            val homeGrid = v.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.home_team_grid)
            val tvSelectedTitle = v.findViewById<TextView>(R.id.tv_selected_team_title)
            refreshSelectedTeam(homeGrid, tvSelectedTitle)
        }
    }

    private fun refreshSelectedTeam(homeGrid: androidx.gridlayout.widget.GridLayout, tvSelectedTitle: TextView) {
        try {
            val sel = com.example.poketeambuilder.data.teams.TeamManager.getSelectedTeam(requireContext())
            if (sel != null) {
                tvSelectedTitle.text = sel.name
                homeGrid.removeAllViews()

                val visible = sel.pokemons.take(6)
                val count = visible.size
                if (count == 0) {
                    // sin pokémon: mostrar marcador de posición
                    homeGrid.removeAllViews()
                    return
                }

                // adaptar columnas para un diseño limpio
                val columns = when (count) {
                    1 -> 1
                    2 -> 2
                    3 -> 3
                    4 -> 2
                    5 -> 3
                    6 -> 3
                    else -> 3
                }
                homeGrid.columnCount = columns
                homeGrid.rowCount = (Math.ceil(count / columns.toDouble())).toInt()

                val size = (80 * resources.displayMetrics.density).toInt()
                val margin = (8 * resources.displayMetrics.density).toInt()

                visible.forEach { p ->
                    val iv = android.widget.ImageView(requireContext()).apply {
                        layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                            width = size
                            height = size
                            setMargins(margin, margin, margin, margin)
                        }
                        scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                        if (p.imageUrl.isNotEmpty()) this.load(p.imageUrl) else setImageResource(R.drawable.ic_launcher_foreground)
                        contentDescription = p.name
                    }
                    homeGrid.addView(iv)
                }

            } else {
                // sin selección: mostrar marcador de posición
                tvSelectedTitle.text = "Ningún equipo seleccionado"
                homeGrid.removeAllViews()
            }
        } catch (e: Exception) {
            // ignorar errores en la vista previa
        }
    }

    private fun captureViewBitmap(view: View): android.graphics.Bitmap {
        val bm = android.graphics.Bitmap.createBitmap(view.width.takeIf { it>0 } ?: 1, view.height.takeIf { it>0 } ?: 1, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bm)
        view.draw(canvas)
        return bm
    }

    private suspend fun saveBitmapToGallery(bitmap: android.graphics.Bitmap, displayName: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val tag = "HomeFragment.saveBitmap"
        try {
            if (bitmap.width <= 1 || bitmap.height <= 1) {
                Log.e(tag, "Bitmap has invalid dimensions: ${bitmap.width}x${bitmap.height}")
                return@withContext false
            }

            val resolver = requireContext().contentResolver

            // API 29+: usar MediaStore con RELATIVE_PATH (no requiere permiso de almacenamiento)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/TuApp")
                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                }
                val collection = android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri: Uri? = resolver.insert(collection, contentValues)
                if (itemUri == null) {
                    Log.e(tag, "Failed to create MediaStore entry")
                    return@withContext false
                }
                resolver.openOutputStream(itemUri)?.use { out ->
                    val ok = bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                    if (!ok) {
                        Log.e(tag, "Failed to compress/write bitmap to stream")
                        resolver.delete(itemUri, null, null)
                        return@withContext false
                    }
                } ?: run {
                    Log.e(tag, "Could not open output stream for uri $itemUri")
                    resolver.delete(itemUri, null, null)
                    return@withContext false
                }
                // marcar como no pendiente
                val update = android.content.ContentValues().apply { put(android.provider.MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(itemUri, update, null, null)
                Log.d(tag, "Saved image to MediaStore: $itemUri")
                return@withContext true
            } else {
                // Legado: escribir en Pictures/TuApp y escanear archivo (requiere WRITE_EXTERNAL_STORAGE en versiones antiguas)
                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val dir = java.io.File(picturesDir, "TuApp")
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, displayName)
                try {
                    java.io.FileOutputStream(file).use { out ->
                        val ok = bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                        if (!ok) {
                            Log.e(tag, "Failed to compress/write bitmap to file ${file.absolutePath}")
                            return@withContext false
                        }
                    }
                    // hacer visible en la galería escaneando el archivo
                    android.media.MediaScannerConnection.scanFile(requireContext(), arrayOf(file.absolutePath), arrayOf("image/png")) { path, uri ->
                        Log.d(tag, "Scanned file: $path -> $uri")
                    }
                    Log.d(tag, "Saved image to file: ${file.absolutePath}")
                    return@withContext true
                } catch (e: Exception) {
                    Log.e(tag, "Error writing file: ${e.message}", e)
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error saving bitmap: ${e.message}", e)
            return@withContext false
        }
    }

}
