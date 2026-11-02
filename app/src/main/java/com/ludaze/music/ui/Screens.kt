package com.ludaze.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.composed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.ludaze.music.MainViewModel
import com.ludaze.music.PlayerUi
import com.ludaze.music.audio.Effects
import com.ludaze.music.data.PlaylistEntity
import com.ludaze.music.data.SongEntity

fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudazeApp(vm: MainViewModel) {
    val songs by vm.songs.collectAsState()
    val favs by vm.favorites.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val ui by vm.ui.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var openPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<PlaylistEntity?>(null) }   // id=-1 => new
    var addTarget by remember { mutableStateOf<SongEntity?>(null) }
    val plSongs by (openPlaylist?.let { vm.playlistSongs(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(emptyList())
    val favIds = remember(favs) { favs.map { it.id }.toSet() }

    BackHandlerCompat(expanded || openPlaylist != null) { if (expanded) expanded = false else openPlaylist = null }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Text("Ludaze Music", fontSize = 26.sp, fontWeight = FontWeight.Light, color = RoseGold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
            TabRow(tab, containerColor = Bg, contentColor = Accent) {
                listOf("Tracks", "Favorites", "Playlists").forEachIndexed { i, t ->
                    Tab(tab == i, { tab = i; openPlaylist = null }, text = { Text(t) },
                        selectedContentColor = Accent, unselectedContentColor = Slate)
                }
            }
            Box(Modifier.weight(1f)) {
                when {
                    tab == 0 -> TrackList(songs, ui, favIds, vm, { addTarget = it }, emptyText = "No audio found on this device")
                    tab == 1 -> TrackList(favs, ui, favIds, vm, { addTarget = it }, emptyText = "Tap the heart on a track to add it here")
                    openPlaylist != null -> Column {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton({ openPlaylist = null }) { Icon(Icons.Default.ArrowBack, null, tint = Accent) }
                            Text(openPlaylist!!.name, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        }
                        TrackList(plSongs, ui, favIds, vm, { addTarget = it }, emptyText = "Empty playlist. Add tracks from the Tracks tab (⋮)")
                    }
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            ListItem({ Text("New playlist", color = Accent) },
                                leadingContent = { Icon(Icons.Default.Add, null, tint = Accent) },
                                colors = ListItemDefaults.colors(containerColor = Bg),
                                modifier = Modifier.clickable { editor = PlaylistEntity(-1, "") })
                        }
                        items(playlists, key = { it.id }) { p ->
                            ListItem({ Text(p.name) },
                                leadingContent = { Icon(Icons.Default.QueueMusic, null, tint = Slate) },
                                trailingContent = { IconButton({ editor = p }) { Icon(Icons.Default.Edit, null, tint = Slate) } },
                                colors = ListItemDefaults.colors(containerColor = Bg),
                                modifier = Modifier.clickable { openPlaylist = p })
                        }
                    }
                }
            }
            if (ui.currentId != null) MiniPlayer(ui, vm) { expanded = true }
            Spacer(Modifier.navigationBarsPadding())
        }
        AnimatedExpand(expanded) { NowPlaying(ui, vm, favIds) { expanded = false } }
    }

    editor?.let { p ->
        var name by remember(p) { mutableStateOf(p.name) }
        ModalBottomSheet({ editor = null }, containerColor = Panel) {
            Column(Modifier.padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (p.id == -1L) "New playlist" else "Edit playlist", fontSize = 18.sp)
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({
                        if (name.isNotBlank()) { if (p.id == -1L) vm.createPlaylist(name) else vm.renamePlaylist(p, name); editor = null }
                    }) { Text("Save") }
                    if (p.id != -1L) OutlinedButton({ vm.deletePlaylist(p.id); if (openPlaylist?.id == p.id) openPlaylist = null; editor = null }) {
                        Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Delete")
                    }
                }
            }
        }
    }
    addTarget?.let { s ->
        ModalBottomSheet({ addTarget = null }, containerColor = Panel) {
            Column(Modifier.padding(bottom = 24.dp).navigationBarsPadding()) {
                Text("Add “${s.title}” to…", Modifier.padding(20.dp), fontSize = 16.sp)
                if (playlists.isEmpty()) Text("Create a playlist first in the Playlists tab.", Modifier.padding(horizontal = 20.dp), color = Slate)
                playlists.forEach { p ->
                    ListItem({ Text(p.name) }, colors = ListItemDefaults.colors(containerColor = Panel),
                        modifier = Modifier.clickable { vm.addToPlaylist(p.id, s.id); addTarget = null })
                }
                openPlaylist?.let { cur ->
                    if (plSongs.any { it.id == s.id }) ListItem({ Text("Remove from ${cur.name}", color = Accent) },
                        colors = ListItemDefaults.colors(containerColor = Panel),
                        modifier = Modifier.clickable { vm.removeFromPlaylist(cur.id, s.id); addTarget = null })
                }
            }
        }
    }
}

@Composable
private fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) =
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)

@Composable
private fun AnimatedExpand(visible: Boolean, content: @Composable () -> Unit) =
    androidx.compose.animation.AnimatedVisibility(
        visible,
        enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut()
    ) { content() }

@Composable
fun TrackList(
    list: List<SongEntity>, ui: PlayerUi, favIds: Set<Long>, vm: MainViewModel,
    onMore: (SongEntity) -> Unit, emptyText: String
) {
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text(emptyText, color = Slate) }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(list, key = { _, s -> s.id }) { i, s ->
            val cur = ui.currentId == s.id
            ListItem(
                headlineContent = { Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (cur) Accent else Color.Unspecified) },
                supportingContent = { Text("${s.artist} · ${fmt(s.durationMs)}", maxLines = 1, color = Slate) },
                leadingContent = {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (cur) Accent else Color.Transparent))
                },
                trailingContent = {
                    Row {
                        IconButton({ vm.toggleFavorite(s.id) }) {
                            Icon(if (s.id in favIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                                tint = if (s.id in favIds) Accent else Slate)
                        }
                        IconButton({ onMore(s) }) { Icon(Icons.Default.MoreVert, null, tint = Slate) }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Bg),
                modifier = Modifier.clickable { vm.play(list, i) }
            )
        }
    }
}

@Composable
fun MiniPlayer(ui: PlayerUi, vm: MainViewModel, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel).clickable { onOpen() }) {
        LinearProgressIndicator({ if (ui.dur > 0) ui.pos.toFloat() / ui.dur else 0f },
            Modifier.fillMaxWidth().height(2.dp), color = Accent, trackColor = Bg)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ui.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ui.artist, maxLines = 1, fontSize = 12.sp, color = Slate)
            }
            IconButton({ vm.prev() }) { Icon(Icons.Default.SkipPrevious, null) }
            IconButton({ vm.toggle() }) { Icon(if (ui.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Accent) }
            IconButton({ vm.next() }) { Icon(Icons.Default.SkipNext, null) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlaying(ui: PlayerUi, vm: MainViewModel, favIds: Set<Long>, onClose: () -> Unit) {
    var showEq by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }
    Box(Modifier.fillMaxSize().background(Bg)) {
        SparkleVisualizer(ui.isPlaying)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            Row {
                IconButton(onClose) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Accent, modifier = Modifier.size(32.dp)) }
                Spacer(Modifier.weight(1f))
                IconButton({ showEq = true }) { Icon(Icons.Default.Equalizer, null, tint = Accent) }
            }
            Spacer(Modifier.weight(1f))
            Text(ui.title, fontSize = 24.sp, fontWeight = FontWeight.Medium, maxLines = 2)
            Text(ui.artist, color = Slate, fontSize = 15.sp)
            Slider(
                value = drag ?: if (ui.dur > 0) ui.pos.toFloat() / ui.dur else 0f,
                onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { vm.seek((it * ui.dur).toLong()) }; drag = null },
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Slate.copy(alpha = .4f))
            )
            Row(Modifier.fillMaxWidth()) {
                Text(fmt(((drag ?: (ui.pos.toFloat() / ui.dur.coerceAtLeast(1))) * ui.dur).toLong()), fontSize = 12.sp, color = Slate)
                Spacer(Modifier.weight(1f)); Text(fmt(ui.dur), fontSize = 12.sp, color = Slate)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton({ vm.prev() }, Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, null, Modifier.size(36.dp)) }
                FilledIconButton({ vm.toggle() }, Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Accent, contentColor = Bg)) {
                    Icon(if (ui.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(40.dp))
                }
                IconButton({ vm.next() }, Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, null, Modifier.size(36.dp)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton({ vm.toggleShuffle() }) { Icon(Icons.Default.Shuffle, null, tint = if (ui.shuffle) Accent else Slate) }
                IconButton({ vm.cycleRepeat() }) {
                    Icon(if (ui.repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, null,
                        tint = if (ui.repeat == Player.REPEAT_MODE_OFF) Slate else Accent)
                }
                Box {
                    TextButton({ speedMenu = true }) { Text("${ui.speed}x", color = Accent) }
                    DropdownMenu(speedMenu, { speedMenu = false }) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                            DropdownMenuItem({ Text("${s}x", color = if (s == ui.speed) Accent else Color.Unspecified) }, { vm.setSpeed(s); speedMenu = false })
                        }
                    }
                }
                val fav = ui.currentId in favIds
                IconButton({ ui.currentId?.let { vm.toggleFavorite(it) } }) {
                    Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (fav) Accent else Slate)
                }
            }
        }
    }
    if (showEq) ModalBottomSheet({ showEq = false }, containerColor = Panel) { EqualizerSheet() }
}

@Composable
fun EqualizerSheet() {
    val ready by Effects.ready.collectAsState()
    val eq = remember(ready) { Effects.eq }
    val bass = remember(ready) { Effects.bass }
    Column(Modifier.padding(20.dp).navigationBarsPadding().verticalScrollCompat(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Equalizer", fontSize = 18.sp, modifier = Modifier.weight(1f))
            TextButton({ Effects.resetAll() }) { Text("Reset", color = Accent) }
        }
        if (eq == null) { Text("Start playback to enable the equalizer.", color = Slate); return@Column }
        val range = eq.bandLevelRange
        for (b in 0 until minOf(5, eq.numberOfBands.toInt())) {
            val band = b.toShort()
            var v by remember(ready, b) { mutableFloatStateOf(eq.getBandLevel(band).toFloat()) }
            val hz = eq.getCenterFreq(band) / 1000
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (hz >= 1000) "${hz / 1000}k" else "$hz", Modifier.width(44.dp), fontSize = 12.sp, color = Slate)
                Slider(v, { v = it; Effects.setBand(b, it.toInt()) },
                    valueRange = range[0].toFloat()..range[1].toFloat(),
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent))
            }
        }
        if (bass != null && bass.strengthSupported) {
            var s by remember(ready) { mutableFloatStateOf(bass.roundedStrength.toFloat()) }
            Text("Bass boost", color = Slate)
            Slider(s, { s = it; Effects.setBass(it.toInt()) }, valueRange = 0f..1000f,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent))
        }
    }
}

private fun Modifier.verticalScrollCompat(): Modifier = composed { this.verticalScroll(rememberScrollState()) }
