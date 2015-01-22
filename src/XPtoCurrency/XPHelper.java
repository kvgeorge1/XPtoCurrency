package XPtoCurrency;

public class XPHelper {
	public static int findLevel(int xp) {
		int lvl = 0;
		int lvlXp = 0;

		if (xp < 7) {
			return 1;
		}

		while (xp > lvlXp) {
			lvl++;
			if (lvl < 16) {
				lvlXp += 2 * lvl + 7;
			} else if (lvl >= 16 && lvl < 31) {
				lvlXp += 5 * lvl - 38;
			} else {
				lvlXp += 9 * lvl - 158;
			}
		}

		return lvl;
	}

	public static int findRemainingXp(int lvlsToRemove, int startingXp) {
		// First, find the level the user currently is at
		//
		int lvl = findLevel(startingXp);
		return 0;
	}
	
	public static void main(String[] args) {
		int xpLevel = 2;
		System.out.println(xpLevel >= 15 ? 37 + (xpLevel - 15) * 5 : xpLevel >= 30 ? 112 + (xpLevel - 30) * 9 : 7 + xpLevel * 2);		
	}
}
