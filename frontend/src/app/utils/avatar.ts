export function initialsAvatarUrl(fullName: string): string {
  const seed = (fullName || 'Usuario').trim() || 'Usuario';
  return `https://api.dicebear.com/7.x/initials/svg?backgroundColor=c5cae9&seed=${encodeURIComponent(seed)}`;
}

export function hasCustomAvatar(avatarUrl?: string | null): boolean {
  return !!avatarUrl?.trim();
}

export function displayAvatarUrl(avatarUrl: string | null | undefined, fullName: string): string {
  return hasCustomAvatar(avatarUrl) ? avatarUrl!.trim() : initialsAvatarUrl(fullName);
}
