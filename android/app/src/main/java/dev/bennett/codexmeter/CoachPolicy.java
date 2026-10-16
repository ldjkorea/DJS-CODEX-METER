package dev.bennett.codexmeter;
import android.content.Context;
/** A fixed character; the user can turn it off, never replace built-in copy. */
final class CoachPolicy {
    static String tone(Context c){return FunStore.tone(c).equals("off")?"off":"spicy";}
}
