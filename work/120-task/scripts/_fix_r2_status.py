"""One-shot script: flip R2 stories from Planned to Done in data.py."""
import pathlib

DATA = pathlib.Path(__file__).with_name("data.py")

content = DATA.read_text(encoding="utf-8")

# Boundaries
r2_start = content.index("RELEASE 2 \u2014 Growth")
r3_start = content.index("RELEASE 3 \u2014 AI + Mobile")

r2_section = content[r2_start:r3_start]
old = '"📋 Planned"'
new = '"✅ Done"'

count = r2_section.count(old)
print(f"R2 section: {count} stories to flip from Planned -> Done")

r2_updated = r2_section.replace(old, new)
new_content = content[:r2_start] + r2_updated + content[r3_start:]

# Sanity
remaining = new_content[r2_start:r3_start].count(old)
done = new_content[r2_start:r3_start].count(new)
print(f"After: {done} Done, {remaining} still Planned in R2 section")

DATA.write_text(new_content, encoding="utf-8")
print("data.py updated.")
