#!/usr/bin/env python3
"""
Firebase Custom Claims Manager for RhythmicRush Admin Portal

Usage:
    python firebase_claims.py set_admin <user_email_or_uid>
    python firebase_claims.py list_admins
    python firebase_claims.py remove_admin <user_email_or_uid>
    python firebase_claims.py check <user_email_or_uid>

Setup:
    1. Install Firebase Admin SDK:
       pip install firebase-admin

    2. Download your Firebase service account key:
       - Go to Firebase Console > Project Settings > Service Accounts
       - Click "Generate new private key"
       - Save as serviceAccountKey.json in this directory

    3. Run commands with your Firebase project
"""

import json
import sys
import os
from pathlib import Path
from typing import Optional

try:
    import firebase_admin
    from firebase_admin import credentials, auth
except ImportError:
    print("❌ Firebase Admin SDK not installed!")
    print("Install it with: pip install firebase-admin")
    sys.exit(1)


class FirebaseClaimsManager:
    def __init__(self, service_account_path: str = "serviceAccountKey.json"):
        self.service_account_path = service_account_path
        self.app = None

    def connect(self) -> bool:
        """Connect to Firebase using service account credentials."""
        if not Path(self.service_account_path).exists():
            print(f"❌ Service account key not found: {self.service_account_path}")
            print("\nTo fix this:")
            print("1. Go to Firebase Console > Your Project > Project Settings")
            print("2. Click 'Service Accounts' tab")
            print("3. Click 'Generate New Private Key'")
            print("4. Save the downloaded JSON file as serviceAccountKey.json")
            return False

        try:
            if not firebase_admin._apps:
                cred = credentials.Certificate(self.service_account_path)
                self.app = firebase_admin.initialize_app(cred)
            else:
                self.app = firebase_admin.get_app()
            print("✅ Connected to Firebase")
            return True
        except Exception as e:
            print(f"❌ Failed to connect to Firebase: {e}")
            return False

    def get_user(self, identifier: str) -> Optional[dict]:
        """Get user by email or UID."""
        try:
            # Try as email first
            if "@" in identifier:
                user = auth.get_user_by_email(identifier)
                print(f"✅ Found user by email: {identifier}")
            else:
                # Try as UID
                user = auth.get_user(identifier)
                print(f"✅ Found user by UID: {identifier}")
            return user
        except auth.UserNotFoundError:
            print(f"❌ User not found: {identifier}")
            return None
        except Exception as e:
            print(f"❌ Error looking up user: {e}")
            return None

    def set_admin(self, identifier: str) -> bool:
        """Grant admin privileges to a user."""
        if not self.connect():
            return False

        user = self.get_user(identifier)
        if not user:
            return False

        try:
            # Set custom claims
            auth.set_custom_user_claims(user.uid, {
                "admin": True,
                "rhythmic_admin": True
            })
            print(f"\n✅ Successfully set admin claims for user:")
            print(f"   UID: {user.uid}")
            print(f"   Email: {user.email or '(no email)'}")
            print(f"   Claims: {{'admin': true, 'rhythmic_admin': true}}")
            print(f"\n📝 Note: Changes take effect on user's next login")
            return True
        except Exception as e:
            print(f"❌ Failed to set claims: {e}")
            return False

    def remove_admin(self, identifier: str) -> bool:
        """Revoke admin privileges from a user."""
        if not self.connect():
            return False

        user = self.get_user(identifier)
        if not user:
            return False

        try:
            # Delete custom claims (empty dict = no claims)
            auth.delete_custom_user_claims(user.uid)
            print(f"\n✅ Successfully removed admin claims for user:")
            print(f"   UID: {user.uid}")
            print(f"   Email: {user.email or '(no email)'}")
            print(f"\n📝 Note: Changes take effect on user's next login")
            return True
        except Exception as e:
            print(f"❌ Failed to remove claims: {e}")
            return False

    def check_claims(self, identifier: str) -> bool:
        """Check if user has admin claims."""
        if not self.connect():
            return False

        user = self.get_user(identifier)
        if not user:
            return False

        custom_claims = user.custom_claims or {}
        is_admin = custom_claims.get("admin", False) or custom_claims.get("rhythmic_admin", False)

        print(f"\n📋 User Details:")
        print(f"   UID: {user.uid}")
        print(f"   Email: {user.email or '(no email)'}")
        print(f"   Email Verified: {'✅' if user.email_verified else '❌'}")
        print(f"   Disabled: {'Yes' if user.disabled else 'No'}")
        print(f"   Custom Claims: {json.dumps(custom_claims, indent=4)}")
        print(f"\n   Admin Status: {'🔑 ADMIN' if is_admin else '👤 Regular User'}")
        return True

    def list_admins(self) -> bool:
        """List all admin users."""
        if not self.connect():
            return False

        try:
            print("\n📋 Fetching all users...")
            page = auth.list_users()
            admins = []

            while page:
                for user in page.users:
                    custom_claims = user.custom_claims or {}
                    if custom_claims.get("admin") or custom_claims.get("rhythmic_admin"):
                        admins.append(user)
                page = page.get_next_page()

            if not admins:
                print("❌ No admins found in this Firebase project")
                return True

            print(f"\n✅ Found {len(admins)} admin(s):\n")
            for user in admins:
                print(f"  • {user.email or user.uid}")
                print(f"    UID: {user.uid}")
                print(f"    Email Verified: {'✅' if user.email_verified else '❌'}")
                print(f"    Custom Claims: {json.dumps(user.custom_claims or {})}")
                print()

            return True
        except Exception as e:
            print(f"❌ Error listing users: {e}")
            return False


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(0)

    command = sys.argv[1].lower()
    manager = FirebaseClaimsManager()

    if command == "set_admin":
        if len(sys.argv) < 3:
            print("❌ Usage: python firebase_claims.py set_admin <user_email_or_uid>")
            sys.exit(1)
        identifier = sys.argv[2]
        success = manager.set_admin(identifier)
        sys.exit(0 if success else 1)

    elif command == "remove_admin":
        if len(sys.argv) < 3:
            print("❌ Usage: python firebase_claims.py remove_admin <user_email_or_uid>")
            sys.exit(1)
        identifier = sys.argv[2]
        success = manager.remove_admin(identifier)
        sys.exit(0 if success else 1)

    elif command == "check":
        if len(sys.argv) < 3:
            print("❌ Usage: python firebase_claims.py check <user_email_or_uid>")
            sys.exit(1)
        identifier = sys.argv[2]
        success = manager.check_claims(identifier)
        sys.exit(0 if success else 1)

    elif command == "list_admins":
        success = manager.list_admins()
        sys.exit(0 if success else 1)

    else:
        print(f"❌ Unknown command: {command}")
        print(__doc__)
        sys.exit(1)


if __name__ == "__main__":
    main()

