interface Tree<out D, out T: Tree<D, T>> {
    val data: D?
    val children: Map<String, T>
}

interface MutableDataTree<D, out T: MutableDataTree<D, T>> : Tree<D, T> {
    override var data: D?
}

interface MutableTree< D, T: MutableTree<D, T>> : MutableDataTree<D, T> {
    override val children: MutableMap<String, T>
    fun getOrCreate(name: String): T
    fun set(name: String, node: T)
}

open class MapTree<D, T: Tree<D, T>>(
    override val data: D?,
    override val children: Map<String, T> = mapOf()
): Tree<D, T>

open class MapMutableDataTree<D, T: MutableDataTree<D, T>>(
    override var data: D? = null,
    override val children: Map<String, T> = mapOf()
) : MapTree<D, T>(data, children),
    MutableDataTree<D, T>

open class MapMutableTree<D>(
    override var data: D? = null,
    override val children: MutableMap<String, MapMutableTree<D>> = mutableMapOf()
) : MapMutableDataTree<D, MapMutableTree<D>>(data, children),
    MutableTree<D, MapMutableTree<D>> {
    override fun getOrCreate(name: String): MapMutableTree<D> =
        children.getOrPut(name) { MapMutableTree() }

    override fun set(name: String, node: MapMutableTree<D>) {
        children[name] = node
    }
}

fun <D, T: MutableTree<D, T>> T.setValue(names: List<String>, value: D)  {
    getOrCreate(names).data = value
}

fun <D, T: MutableTree<D, T>> T.getOrCreate(names: List<String>): T =
    when (names.size) {
        0 -> this
        else -> getOrCreate(names.first()).getOrCreate(names.drop(1))
    }

fun <D, T: MutableTree<D, T>> T.createCopy(source: T, names: List<String>)  {
    val local = getOrCreate(names.first()).getOrCreate(names.drop(1))
    local.data  = source.data
    when (source.children.size) {
        0 -> Unit
        else -> source.children.forEach {
            local.createCopy(it.value, listOf(it.key) + names.drop(1))
        }
    }
}

fun <D, T: MutableTree<D, T>> T.copyBranch(names: List<String>, branch: T) {
    createCopy(branch, names)
}

fun <D, T: Tree<D, T>> T.visit(action: (path: List<String>, value: D?) -> Unit) {
    fun T.visitTree(names: List<String>) {
        action(names, data)
        children.forEach { (token, branch) ->
            branch.visitTree(names + token)
        }
    }
    visitTree(listOf("root"))
}

fun runV1() {
    // Tree -> MutableDataTree -> MutableTree
    // TreeImpl  -> MutableDataTreeImpl -> MapMutableTree
    val mapMutableTree: MutableTree<Number, *> = MapMutableTree(2, mutableMapOf())
    val mapMutableTree2:  MapMutableTree<Number> = MapMutableTree(4, mutableMapOf())
    val mapMutableDataTree2: MutableDataTree<Number, *> = MapMutableTree(3, mutableMapOf("mapMutableTree2" to mapMutableTree2))
}

fun runV2() {
    //unable to add it to any tree except immutableTree
    val immutableTreeEmpty: Tree<Number, MutableTree<Number, *>> = MapTree(null, mapOf())

    val mapMutableTree1: MapMutableTree<Number>  = MapMutableTree(1, mutableMapOf())
    val mapMutableTree2: MapMutableTree<Number>  = MapMutableTree(2, mutableMapOf("mapMutableTree1" to mapMutableTree1))
    val mapMutableTree3: MapMutableTree<Number>  = MapMutableTree(3, mutableMapOf("mapMutableTree2" to mapMutableTree2))
    val mapMutableTree4: MapMutableTree<Number>  = MapMutableTree(4, mutableMapOf("mapMutableTree3" to mapMutableTree3))

    val mapMutableTree5: MapMutableTree<Number>  = MapMutableTree(5, mutableMapOf())
    mapMutableTree5.copyBranch(listOf("mapMutableTree4"), mapMutableTree4)

    mapMutableTree5.visit { path, value ->
        println("$path: $value")
    }

    val mapMutableTree: MutableTree<Number, *>  = MapMutableTree(2, mutableMapOf())
    val mutableDataTree: MutableDataTree<Number, *> = MapMutableDataTree(1, mapOf("mapMutableTree" to mapMutableTree))
    val immutableTree: Tree<Number, *> = MapTree(null, mapOf("mutableDataTree" to mutableDataTree))

    immutableTree.visit { path, value ->
        println("$path: $value")
    }
}

fun main() {
    runV2()
}