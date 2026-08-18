package com.lorecanvas.commands

/**
 * Commands skeleton (PEP-001, Phase 1).
 *
 * Implements the shape of LCD-009, Chapter 18 (Undo/Redo Workflow):
 * "Undo Request -> History Stack -> Restore Previous State -> Repository ->
 * Publish Event -> Refresh UI." Every user action that mutates project data
 * should be expressed as a Command so it is undoable by construction, rather
 * than undo being bolted on afterward per-feature.
 */
interface Command {
    /** Human-readable label for undo/redo menu items, e.g. "Create Node". */
    val label: String

    fun execute()
    fun undo()
}

/**
 * Groups several Commands into one undo/redo step (Phase 7 — "Batch
 * commands"). Executes its children in order; undoes them in *reverse*
 * order, since a later command may depend on state an earlier one in the
 * same batch established (e.g. "create Node, then add three default
 * Cards to it" — undoing must remove the Cards before the Node they
 * belong to would even make sense without, though in this app's case
 * Node deletion isn't itself a Command — see [com.lorecanvas.repository.CreateNodeCommand]'s
 * own doc comment on why. The ordering guarantee here is what makes any
 * future batch that *does* have such dependencies safe to undo.
 */
class CompoundCommand(override val label: String, private val commands: List<Command>) : Command {
    override fun execute() {
        commands.forEach { it.execute() }
    }

    override fun undo() {
        commands.asReversed().forEach { it.undo() }
    }
}

/**
 * A bounded-depth undo/redo history stack. Phase 1 keeps this in-memory only;
 * persisting history across app restarts (if ever desired) is out of scope
 * here per LCD-017's Undo/Redo acceptance criteria, which only requires
 * correctness within a single session.
 */
class CommandHistory(private val maxDepth: Int = 100) {
    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** What Undo would do next, for UI labels like "Undo Rename Node." */
    val nextUndoLabel: String? get() = undoStack.lastOrNull()?.label
    val nextRedoLabel: String? get() = redoStack.lastOrNull()?.label

    /** Executed commands, oldest first — backs a "Command History" list view. */
    fun executedLabels(): List<String> = undoStack.map { it.label }

    fun execute(command: Command) {
        command.execute()
        undoStack.addLast(command)
        if (undoStack.size > maxDepth) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    /** Convenience for executing several Commands as one undo step — see [CompoundCommand]. */
    fun executeBatch(label: String, commands: List<Command>) {
        execute(CompoundCommand(label, commands))
    }

    fun undo() {
        val command = undoStack.removeLastOrNull() ?: return
        command.undo()
        redoStack.addLast(command)
    }

    fun redo() {
        val command = redoStack.removeLastOrNull() ?: return
        command.execute()
        undoStack.addLast(command)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
