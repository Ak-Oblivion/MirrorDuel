package mirrorduel;

public class GameEnums {

    public enum Direction {
        UP, DOWN, LEFT, RIGHT;

        public Direction reflectSlash() {
            // Slash mirror /
            return switch (this) {
                case UP    -> RIGHT;
                case DOWN  -> LEFT;
                case LEFT  -> DOWN;
                case RIGHT -> UP;
            };
        }

        public Direction reflectBackslash() {
            // Backslash mirror \
            return switch (this) {
                case UP    -> LEFT;
                case DOWN  -> RIGHT;
                case LEFT  -> UP;
                case RIGHT -> DOWN;
            };
        }

        public int dx() {
            return switch (this) {
                case LEFT  -> -1;
                case RIGHT ->  1;
                default    ->  0;
            };
        }

        public int dy() {
            return switch (this) {
                case UP    -> -1;
                case DOWN  ->  1;
                default    ->  0;
            };
        }
    }

    public enum PieceType {
        EMITTER, CRYSTAL, MIRROR_SLASH, MIRROR_BACKSLASH
    }

    public enum PlayerID {
        RED, BLUE;

        public PlayerID opponent() {
            return this == RED ? BLUE : RED;
        }
    }

    public enum GameScreen {
        MAIN_MENU, RULES, PLAYING, VICTORY
    }
}
