import pathlib
import sys

# Make the top-level repo importable so `from db.migrate import ...`
# resolves without needing a packaging setup.
ROOT = pathlib.Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
