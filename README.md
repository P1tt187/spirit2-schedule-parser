# spirit2-schedule-parser

this Programm parses the schedule from [http://my.fh-sm.de/~fbi-x/Stundenplan/](http://my.fh-sm.de/~fbi-x/Stundenplan/) and transforms it to a JSON-Array

## Outputstructure
```javascript

{
  "title": "", // title is set when the kind is a block
  "scheduleData": [
    {
      "alternatives": [], 
      "duration": "UNEVEN",
      "course": [
        ""
      ],
      //generated sha512 checksum
      "uuid": "a6f63654b466fab00d953a14c859680a4a7f081ff93a7381898fae577dd8875e9a9847e3cc89f9943c0e69b3f92ac8665b5bfd7404dbe86d10ab80ab782120fc",
      "groupIndex": "",
      "scheduleKind": "REGULAR",
      "docents": [
        "Recknagel"
      ],
      "time": {
        "startMinute": 15,
        "stopHour": 9,
        "stopMinute": 45,
        "weekday": "TUESDAY",
        "startHour": 8
      },
      "room": "H0002",
      "longTitle": "",
      "lectureKind": "LECTURE",
      "lectureName": "Mathe 1 V"
    },
    {
    "alternatives": [
      {
        "duration": "WEEKLY",
        "hour": "3",
        "weekday": "WEDNESDAY",
        "room": "WKST",
        "lecture": "DBS V1"
      }
    ],
    "duration": "WEEKLY",
    "course": [
      ""
    ],
    "uuid": "bdd18e36e7339de9442887aae190781f321f47cb45afcb4c42baa80001a6f0facd93f3e8e8a133dca2026c759968d8cdf7a4f2231cf22dca74cfb59d87eaeb2e",
    "groupIndex": "",
    "scheduleKind": "REGULAR",
    "docents": [
      "heimrich"
    ],
    "time": {
      "startMinute": 45,
      "stopHour": 13,
      "stopMinute": 15,
      "weekday": "WEDNESDAY",
      "startHour": 11
    },
    "room": "H0203*",
    "longTitle": "",
    "lectureKind": "LECTURE",
    "lectureName": "DBS V1"
  }],  
  "uid":"bdd18e36e7339de9442887aae190781f321f47cb45afcb4c42baa80001a6f0facd93f3e8e8a133dca2026c759968d8cdf7a4f2231cf22dca74cfb59d87eaeb2e"
    }

```

