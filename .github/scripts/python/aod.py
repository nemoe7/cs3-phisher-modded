#!/usr/bin/env python

import json
import requests
import fastjsonschema
import re
from urllib.parse import urlparse

DATA_URL = "https://github.com/manami-project/anime-offline-database/raw/refs/heads/master/anime-offline-database-minified.json"

SCHEMA = {
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://github.com/manami-project/anime-offline-database/raw/master/anime-offline-database.schema.json",
  "title": "anime-offline-database",
  "description": "A JSON based anime dataset containing the most important meta data as well as cross references to various anime sites such as MAL, ANIDB, ANILIST, KITSU and more...",
  "type": "object",
  "required": [
    "license",
    "repository",
    "scoreRange",
    "lastUpdate",
    "data"
  ],
  "properties": {
    "license": {
      "description": "Information about the license of the dataset.",
      "type": "object",
      "required": [
        "name",
        "url"
      ],
      "properties": {
        "name": {
          "description": "Name of the license.",
          "type": "string",
          "const": "Open Data Commons Open Database License (ODbL) v1.0 + Database Contents License (DbCL) v1.0"
        },
        "url": {
          "description": "URL to the license file.",
          "type": "string",
          "format": "uri"
        }
      }
    },
    "repository": {
      "description": "URL of this github repository which is the source of the dataset.",
      "type": "string",
      "const": "https://github.com/manami-project/anime-offline-database"
    },
    "scoreRange": {
      "description": "Describes the upper and lower boundaries of a score.",
      "type": "object",
      "required": [
        "minInclusive",
        "maxInclusive"
      ],
      "properties": {
        "minInclusive": {
          "description": "Minimum value that a score can take.",
          "type": "number"
        },
        "maxInclusive": {
          "description": "Maximum value that a score can take.",
          "type": "number"
        }
      }
    },
    "lastUpdate": {
      "description": "The date on which the file was updated in the format: YYYY-MM-DD.",
      "type": "string",
      "format": "date"
    },
    "data": {
      "description": "List of all anime.",
      "type": "array",
      "uniqueItems": True,
      "items": {
        "type": "object",
        "required": [
          "sources",
          "title",
          "type",
          "episodes",
          "status",
          "animeSeason",
          "picture",
          "thumbnail",
          "synonyms",
          "relatedAnime",
          "tags"
        ],
        "properties": {
          "sources": {
            "description": "URLs to the pages of the meta data providers for this anime.",
            "type": "array",
            "uniqueItems": True,
            "items": {
              "type": "string",
              "format": "uri"
            }
          },
          "title": {
            "description": "Main title.",
            "type": "string"
          },
          "type": {
            "description": "Distribution type.",
            "enum": [
              "TV",
              "MOVIE",
              "OVA",
              "ONA",
              "SPECIAL",
              "UNKNOWN"
            ]
          },
          "episodes": {
            "description": "Number of episodes, movies or parts.",
            "type": "number",
            "minimum": 0
          },
          "status": {
            "description": "Status of distribution.",
            "enum": [
              "FINISHED",
              "ONGOING",
              "UPCOMING",
              "UNKNOWN"
            ]
          },
          "animeSeason": {
            "description": "Data on when the anime was first distributed.",
            "type": "object",
            "required": [
              "season"
            ],
            "properties": {
              "season": {
                "description": "Season.",
                "enum": [
                  "SPRING",
                  "SUMMER",
                  "FALL",
                  "WINTER",
                  "UNDEFINED"
                ]
              },
              "year": {
                "description": "Year.",
                "type": "number",
                "minimum": 1907
              }
            }
          },
          "picture": {
            "description": "URL of a picture which represents the anime.",
            "type": "string",
            "format": "uri"
          },
          "thumbnail": {
            "description": "URL of a smaller version of the picture.",
            "type": "string",
            "format": "uri"
          },
          "duration": {
            "description": "Duration per episode.",
            "type": "object",
            "required": [
              "value",
              "unit"
            ],
            "properties": {
              "value": {
                "description": "Duration in seconds",
                "type": "number",
                "minimum": 1
              },
              "unit": {
                "description": "When (de)serialized the unit is always SECONDS",
                "type": "string",
                "const": "SECONDS"
              }
            }
          },
          "synonyms": {
            "description": "Alternative titles and spellings under which the anime is also known.",
            "type": "array",
            "uniqueItems": True,
            "items": {
              "type": "string"
            }
          },
          "relatedAnime": {
            "description": "URLs to the meta data providers for anime that are somehow related to this anime.",
            "type": "array",
            "uniqueItems": True,
            "items": {
              "type": "string",
              "format": "uri"
            }
          },
          "tags": {
            "description": "A non-curated list of tags and genres which describe the anime.",
            "type": "array",
            "uniqueItems": True,
            "items": {
              "type": "string"
            }
          }
        }
      }
    }
  }
}

KEEP = {"title", "type", "picture", "synonyms"}

ID_HOSTS = {
  "myanimelist.net": "mal",
  "anilist.co": "anilist",
  "kitsu.app": "kitsu"
}

def extract_id(url):
  try:
    parsed = urlparse(url)
    for host, key in ID_HOSTS.items():
      if host in parsed.netloc:
        match = re.search(r"/(\d+)", parsed.path)
        if match:
          return key, match.group(1)
  except Exception:
    pass
  return None, None

def load_json(url):
  resp = requests.get(url)
  resp.raise_for_status()
  return resp.json()

def validate_json(data, schema):
  validate = fastjsonschema.compile(schema)
  try:
    validate(data)
    print("Validation successful.")
  except fastjsonschema.JsonSchemaException as e:
    print(f"Validation failed: {e.message}")
    raise

def strip_data(data):
  stripped = []
  for anime in data["data"]:
    new_entry = {k: v for k, v in anime.items() if k in KEEP}

    ids = {}
    for url in anime.get("sources", []):
      key, val = extract_id(url)
      if key and val:
        ids[key] = val
    if ids:
      new_entry["ids"] = ids

    stripped.append(new_entry)
  return {"data": stripped}

def main():
  print("Loading JSON from URL...")
  data = load_json(DATA_URL)

  print("Validating JSON against schema...")
  validate_json(data, SCHEMA)

  print("Stripping data and extracting IDs...")
  new_data = strip_data(data)

  output_file = "anime-offline-database-trimmed.json"
  with open(output_file, "w", encoding="utf-8") as file:
    json.dump(new_data, file, ensure_ascii=False, separators=(',', ':'))
  print(f"Filtered JSON saved to {output_file}")

if __name__ == "__main__":
  main()
