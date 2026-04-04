import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 100 },
        { duration: '1m',  target: 300 },
        { duration: '1m',  target: 300 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.05'],
    },
};

export default function () {
    const res = http.get('http://localhost:8080/api/v1/properties?page=0&size=50&sort=pricePerNight,desc');

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(0.01);
}


/*
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '2m',  target: 150 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<1500'],
    },
};

const TOKEN = '';

export default function () {
    const rand = Math.random();

    if (rand < 0.70) {
        const res = http.get('http://localhost:8080/api/v1/properties?page=0&size=50&sort=pricePerNight,desc');
        check(res, { 'GET status 200': (r) => r.status === 200 });

    } else if (rand < 0.90) {
        const randomBookingId = Math.floor(Math.random() * 35) + 1;
        const res = http.get(`http://localhost:8080/api/v1/bookings/${randomBookingId}/status`);
        check(res, { 'GET status 200 or 404': (r) => r.status === 200 || r.status === 404 });

    } else {
        const payload = JSON.stringify({
            propertyId: Math.floor(Math.random() * 58) + 1,
            startDate: "2026-06-01",
            endDate: "2026-06-10"
        });

        const params = {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ${TOKEN}'
            },
        };

        const res = http.post('http://localhost:8080/api/v1/bookings', payload, params);

        check(res, { 'POST create status 201/200': (r) => r.status === 201 || r.status === 200 });
    }

    sleep(Math.random() * 2 + 1);
}*/